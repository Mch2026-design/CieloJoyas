"""Pagos, propiedad del pedido y certificado. Usar solo en una base temporal."""
import os,json,uuid,urllib.request,urllib.error,http.cookiejar
assert os.environ.get('CIELO_TEST_ALLOW_MUTATIONS')=='1','Autorizar solo contra una base temporal.'
ROOT=os.environ.get('API_URL','http://127.0.0.1:18081');checks=[]
def check(value,label):
 assert value,label
 checks.append(label);print('OK:',label)
class Client:
 def __init__(self):
  self.jar=http.cookiejar.CookieJar();self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar));self.csrf=None;self.csrf=self.call('GET','/api/auth/me')[1]['csrf']
 def call(self,method,path,body=None):
  headers={'Accept':'application/json'}
  if body is not None:headers['Content-Type']='application/json'
  if method!='GET':headers['X-CSRF-Token']=self.csrf
  request=urllib.request.Request(ROOT+path,method=method,headers=headers,data=None if body is None else json.dumps(body).encode())
  try:
   with self.opener.open(request,timeout=15) as response:
    data=response.read();kind=response.headers.get('Content-Type','');result=json.loads(data) if 'json' in kind else data
    if isinstance(result,dict) and result.get('csrf'):self.csrf=result['csrf']
    return response.status,result,kind
  except urllib.error.HTTPError as error:return error.code,error.read(),error.headers.get('Content-Type','')
a=Client();b=Client();guest=Client();admin=Client()
check(admin.call('POST','/api/auth/login',{'email':'admin-prueba@example.test','password':'CieloPrueba2026Local'})[0]==200,'Acceso administrador')
for client in [a,b]:
 email='pago-'+uuid.uuid4().hex+'@example.test';check(client.call('POST','/api/auth/register',{'firstName':'María','lastName':'Pruebas','email':email,'password':'CieloPrueba2026Local'})[0]==200,'Cuenta de cliente de pruebas')
payment=a.call('GET','/api/config')[1]['payment'];check(payment['holder']=='MARICIELO GIRAO CHACALIAZA' and len(payment['cci'])==20 and payment['demonstration'],'CCI de demostración identificado y titular correcto')
products=a.call('GET','/api/products')[1];product=next(p for p in products if p['stock']>=4)
check(all(p['certificateUrl'] is None for p in products),'Catálogo no publica descargas de certificado antes de comprar')
def create(client,method):
 body={'requestId':str(uuid.uuid4()),'firstName':'María','lastName':'Pruebas','email':'visitante@example.test','phone':'987654321','documentType':'dni','documentId':'12345678','addressLine':'Av. Pruebas 123','ubigeo':'150101','paymentMethod':method,'items':[{'productId':product['id'],'quantity':1}]}
 code,order,_=client.call('POST','/api/orders',body);check(code==200,'Pedido registrado con '+method);return order
for method in ['yape','plin','transferencia']:
 order=create(a,method);oid=order['id'];path=f'/api/orders/{oid}'
 check(not order['certificateAvailable'] and order['certificateUrl'] is None,'Certificado bloqueado mientras el pago está pendiente: '+method)
 check(a.call('GET',path)[0]==200,'La propietaria consulta su pedido: '+method)
 check(b.call('GET',path)[0]==404 and guest.call('GET',path)[0]==404,'Otras sesiones no consultan el pedido: '+method)
 check(a.call('GET',path+'/certificate')[0]==403,'Java bloquea el PDF pendiente: '+method)
 check(admin.call('PATCH',f'/api/admin/orders/{oid}/status',{'status':'pagado'})[0]==200,'Pago confirmado desde administración: '+method)
 updated=a.call('GET',path)[1];check(updated['certificateAvailable'] and updated['certificateUrl']==path+'/certificate','El certificado se habilita al pagar: '+method)
 code,data,kind=a.call('GET',path+'/certificate');check(code==200 and data.startswith(b'%PDF') and 'application/pdf' in kind,'PDF disponible después del pago: '+method)
 check(b.call('GET',path+'/certificate')[0]==404,'Otra clienta no descarga el certificado: '+method)
 check(a.call('GET',path+'/certificate?preview=true')[1].startswith(b'\x89PNG'),'Vista del certificado protegida: '+method)
 check(admin.call('PATCH',f'/api/admin/orders/{oid}/status',{'status':'cancelado'})[0]==200,'Cancelar pago conserva transición válida: '+method)
 check(a.call('GET',path+'/certificate')[0]==403,'Cancelar retira el certificado: '+method)
order=create(guest,'yape');oid=order['id'];check(guest.call('GET',f'/api/orders/{oid}')[0]==200,'Visitante conserva acceso a la compra de su propia sesión')
check(admin.call('PATCH',f'/api/admin/orders/{oid}/status',{'status':'pagado'})[0]==200,'Pago de visitante confirmado')
check(guest.call('GET',f'/api/orders/{oid}/certificate')[0]==200,'Visitante obtiene certificado de su compra confirmada')
for file in ['certificado-material.pdf','certificado-material.png']:
 check(a.call('GET','/assets/'+file)[0]==403,'La URL estática no permite saltar la comprobación: '+file)
 check(admin.call('GET','/assets/'+file)[0]==200,'Administración puede revisar su plantilla: '+file)
check(guest.call('GET','/assets/pago-yape-plin.png')[0]==200,'QR de pago disponible')
print(json.dumps({'result':'OK','checks':len(checks),'tests':checks},ensure_ascii=False,indent=2))
