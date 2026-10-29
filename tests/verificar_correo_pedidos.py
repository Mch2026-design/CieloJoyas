"""Verificación Java/PostgreSQL/SMTP local. Crea y elimina exclusivamente una base temporal."""
import asyncore
import concurrent.futures
import email
from email import policy
import http.cookiejar
from html import unescape
import json
import os
from pathlib import Path
import shutil
import smtpd
import socket
import subprocess
import threading
import time
import urllib.error
import urllib.request
import uuid

assert os.environ.get('CIELO_TEST_ALLOW_MUTATIONS') == '1', 'Autoriza la prueba con CIELO_TEST_ALLOW_MUTATIONS=1.'
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'verificacion'
DB = 'cielo_correo_test_' + uuid.uuid4().hex[:12]
HOST = os.environ.get('CIELO_TEST_PG_HOST', '127.0.0.1')
PG_PORT = os.environ.get('CIELO_TEST_PG_PORT', '5432')
USER = os.environ.get('CIELO_TEST_PG_USER', 'postgres')
psql = shutil.which('psql')
assert psql and shutil.which('mvn') and shutil.which('java'), 'Requiere PostgreSQL, Maven y Java en PATH.'
checks = []
def check(value, label):
    assert value, label
    checks.append(label)
    print('OK:', label, flush=True)
def sql(statement, database=DB):
    return subprocess.check_output([psql, '-w', '-h', HOST, '-p', PG_PORT, '-U', USER, '-d', database,
                                    '-v', 'ON_ERROR_STOP=1', '-Atc', statement], text=True).strip()
def port():
    with socket.socket() as s:
        s.bind(('127.0.0.1', 0))
        return s.getsockname()[1]
class Inbox(smtpd.SMTPServer):
    def __init__(self, smtp_port):
        super().__init__(('127.0.0.1', smtp_port), None, decode_data=False)
        self.messages, self.reject = [], False
    def process_message(self, peer, mailfrom, rcpttos, data, **kwargs):
        if self.reject:
            return '451 Fallo temporal de prueba'
        self.messages.append((rcpttos, email.message_from_bytes(data, policy=policy.default)))
class Client:
    def __init__(self, url):
        self.url, self.csrf = url, None
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.call('GET', '/api/auth/me')
    def call(self, method, path, body=None):
        headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
        if method != 'GET':
            headers['X-CSRF-Token'] = self.csrf
        request = urllib.request.Request(self.url + path, method=method, headers=headers,
                                        data=None if body is None else json.dumps(body).encode())
        with self.opener.open(request, timeout=30) as response:
            result = json.load(response)
            if isinstance(result, dict) and result.get('csrf'):
                self.csrf = result['csrf']
            return result
smtp_port, app_port = port(), port()
smtp = Inbox(smtp_port)
stopped = threading.Event()
def receive():
    while not stopped.is_set():
        asyncore.loop(timeout=0.1, count=1)
thread = threading.Thread(target=receive, daemon=True)
thread.start()
server = None
created = False
try:
    sql('CREATE DATABASE ' + DB, 'postgres')
    created = True
    for name in ['migracion_01_incio.sql', 'migracion_03_correo_pedidos.sql', 'migracion_03_correo_pedidos.sql']:
        result = subprocess.run([psql, '-w', '-h', HOST, '-p', PG_PORT, '-U', USER, '-d', DB,
                                 '-v', 'ON_ERROR_STOP=1', '-f', str(ROOT / 'sql' / name)], capture_output=True, text=True)
        assert result.returncode == 0, result.stderr
    check(sql("SELECT count(*) FROM pg_tables WHERE schemaname='public'") == '12', 'Migración repetible; conserva las 12 tablas')
    env = dict(os.environ, DB_URL=f'jdbc:postgresql://{HOST}:{PG_PORT}/{DB}', DB_USER=USER,
               DB_PASSWORD=os.environ.get('PGPASSWORD', ''), MAIL_ENABLED='false')
    with (OUT / 'maven-correo-completo.txt').open('w') as log:
        result = subprocess.run(['mvn', '-o', '-Dspring.config.import=', '-Dcielo.mail.enabled=false', 'package'],
                                cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
    check(result.returncode == 0, 'Maven package: compilación, 10 pruebas de correo y arranque del contexto Java')
    jars = [p for p in (ROOT / 'target').glob('*.jar') if not p.name.endswith('.original')]
    assert len(jars) == 1
    args = ['java', '-jar', str(jars[0]), '--spring.config.import=', '--server.address=127.0.0.1',
            '--server.port=' + str(app_port), '--cielo.mail.enabled=true', '--cielo.mail.from=tienda@example.test',
            '--spring.mail.host=127.0.0.1', '--spring.mail.port=' + str(smtp_port),
            '--spring.mail.username=', '--spring.mail.password=', '--spring.mail.properties.mail.smtp.auth=false',
            '--spring.mail.properties.mail.smtp.ssl.enable=false', '--spring.mail.properties.mail.smtp.starttls.enable=false',
            '--spring.mail.properties.mail.smtp.starttls.required=false']
    with (OUT / 'servidor-correo-prueba.txt').open('w') as log:
        server = subprocess.Popen(args, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
    url = f'http://127.0.0.1:{app_port}'
    for attempt in range(80):
        try:
            with urllib.request.urlopen(url + '/api/health', timeout=1) as response:
                assert json.load(response)['database'] == DB
                break
        except (OSError, AssertionError):
            assert server.poll() is None, 'Java no pudo iniciar; consulta servidor-correo-prueba.txt'
            time.sleep(0.25)
    else:
        raise AssertionError('Java no respondió')
    check(True, 'JAR inicia con PostgreSQL y SMTP local')
    a, b, guest = Client(url), Client(url), Client(url)
    for client, address in [(a, 'cliente-correo@example.test'), (b, 'otra-cuenta@example.test')]:
        client.call('POST', '/api/auth/register', {'firstName': 'Lucía', 'lastName': 'Pruebas', 'email': address,
                                                 'password': 'CorreoPrueba2026Local'})
    check(True, 'Registro de cuentas con correo válido')
    product = next(p for p in a.call('GET', '/api/products') if p['customizable'] and p['stock'] >= 3)
    payload = {'requestId': str(uuid.uuid4()), 'firstName': 'Lucía', 'lastName': 'Pruebas',
               'email': 'alternativo@example.test', 'phone': '987654321', 'documentType': 'dni',
               'documentId': '12345678', 'addressLine': 'Av. Pruebas 123', 'reference': 'Puerta azul',
               'ubigeo': '150101', 'paymentMethod': 'plin', 'coupon': 'CIELO10',
               'items': [{'productId': product['id'], 'quantity': 2, 'engraving': 'María & Sol', 'size': '18'},
                         {'productId': product['id'], 'quantity': 1, 'engraving': 'Lucía', 'size': '19'}]}
    order = a.call('POST', '/api/orders', payload)
    check(order['emailSent'] and order['emailStatus'] == 'sent', 'SMTP acepta el correo después de guardar el pedido')
    check(order['email'] == 'cliente-correo@example.test', 'Java utiliza el correo de la cuenta y descarta el alternativo')
    recipients, message = smtp.messages[-1]
    check(recipients == ['cliente-correo@example.test'] and message['Cc'] is None and message['Bcc'] is None,
          'Entrega únicamente al correo del comprador')
    check('Cielo Joyas' in str(message['From']) and str(message['Subject']) == f"Pedido #{order['id']} recibido · Cielo Joyas",
          'Remitente y asunto con Cielo Joyas y número de pedido')
    text = message.get_body(preferencelist=('plain',)).get_content()
    html = message.get_body(preferencelist=('html',)).get_content()
    (OUT / 'correo-pedido-vista-previa.html').write_text(html, encoding='utf-8')
    (OUT / 'correo-pedido-vista-previa.txt').write_text(text, encoding='utf-8')
    check(f"2 × {product['name']}" in html and f"1 × {product['name']}" in html, 'Piezas y cantidades en líneas separadas')
    check('María & Sol' in unescape(html) and '&amp;' in html and 'Lucía' in text, 'Personalización y acentos en MIME UTF-8')
    check('Descuento (CIELO10)' in html and f"S/ {float(order['total']):.2f}" in html, 'Descuento y total coinciden con Java')
    address = ', '.join(order[k] for k in ['address', 'district', 'province', 'department'])
    check(address in text and 'Puerta azul' in text and 'Plin' in text,
          'Dirección, localidad, referencia y medio de pago')
    check('pago está pendiente' in text and 'no está incluido' in text and not order['certificateAvailable'],
          'Distingue recepción del pedido, verificación del pago y envío por cotizar')
    repeat = a.call('POST', '/api/orders', payload)
    check(repeat['id'] == order['id'] and len(smtp.messages) == 1, 'Repetir la solicitud no duplica el pedido ni el correo')
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        repeated = list(pool.map(lambda _: a.call('POST', '/api/orders', payload), range(2)))
    check(all(o['id'] == order['id'] for o in repeated) and len(smtp.messages) == 1, 'Reintentos concurrentes no reenvían el aviso')
    history = a.call('GET', '/api/account/orders')
    check(len(history) == 1 and history[0]['emailSent'], 'Historial de la cuenta conserva el envío aceptado')
    try:
        b.call('GET', f"/api/orders/{order['id']}")
        raise AssertionError('Otra cuenta obtuvo el pedido')
    except urllib.error.HTTPError as error:
        check(error.code == 404, 'Otra cuenta no puede leer la compra ni su correo')
    smtp.reject = True
    failed_payload = dict(payload, requestId=str(uuid.uuid4()))
    failed = a.call('POST', '/api/orders', failed_payload)
    check(not failed['emailSent'] and failed['emailStatus'] == 'failed', 'SMTP rechazado se informa sin afirmar envío')
    check(sql(f"SELECT count(*) FROM pedidos WHERE id={failed['id']} AND correo_enviado_en IS NULL") == '1',
          'Fallo SMTP conserva el pedido sin fecha de envío')
    smtp.reject = False
    recovered = a.call('POST', '/api/orders', failed_payload)
    check(recovered['id'] == failed['id'] and recovered['emailSent'] and len(smtp.messages) == 2,
          'Reintentar un fallo SMTP recupera el aviso sin duplicar compra')
    guest_payload = dict(payload, requestId=str(uuid.uuid4()), email='visitante@example.test', paymentMethod='yape')
    visitor = guest.call('POST', '/api/orders', guest_payload)
    check(visitor['emailSent'] and smtp.messages[-1][0] == ['visitante@example.test'], 'Visitante recibe el pedido en el correo de checkout')
    check(sql('SELECT count(*) FROM pedidos') == '3', 'Tres compras guardadas pese a los reintentos y el rechazo SMTP')
    check(sql('SELECT count(*) FROM pedidos WHERE correo_enviado_en IS NOT NULL') == '3', 'Fecha de envío persistida en las tres compras aceptadas')
    (OUT / 'correo-pedidos-resultados.json').write_text(json.dumps({'result': 'OK', 'checks': len(checks), 'tests': checks,
        'smtp': 'local, sin correo externo', 'database': 'temporal eliminada al terminar'}, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({'result': 'OK', 'checks': len(checks)}, ensure_ascii=False), flush=True)
finally:
    if server is not None:
        server.terminate()
        try:
            server.wait(timeout=15)
        except subprocess.TimeoutExpired:
            server.kill()
            server.wait()
    stopped.set()
    thread.join(timeout=2)
    smtp.close()
    if created:
        sql('DROP DATABASE ' + DB, 'postgres')
