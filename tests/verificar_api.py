"""Prueba de integración real. Ejecutar SOLO sobre una base temporal recién cargada.
Requiere CIELO_TEST_ALLOW_MUTATIONS=1 y API_URL.
Crea pedidos, compras y reseñas de prueba; no usar contra datos personales.
"""
import os, json, uuid, urllib.request, urllib.error, http.cookiejar, struct, zlib
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal

assert os.environ.get('CIELO_TEST_ALLOW_MUTATIONS') == '1', 'Usar solo una base temporal y autorizar con CIELO_TEST_ALLOW_MUTATIONS=1.'
URL=os.environ.get('API_URL','http://127.0.0.1:18081')+'/api'
ADMIN_EMAIL='admin-prueba@example.test'
ADMIN_PASSWORD='CieloPrueba2026Local'
checks=[]
def check(condition, label):
    assert condition, label
    checks.append(label)
    print('OK:',label)
class Client:
    def __init__(self):
        self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.csrf=None
        self.csrf=self.call('GET','/auth/me')[1]['csrf']
    def call(self,method,path,data=None):
        headers={'Accept':'application/json'}
        if data is not None:headers['Content-Type']='application/json'
        if method!='GET' and self.csrf:headers['X-CSRF-Token']=self.csrf
        req=urllib.request.Request(URL+path,headers=headers,method=method,data=None if data is None else json.dumps(data).encode())
        try:
            with self.opener.open(req,timeout=20) as r:
                result=json.load(r)
                if isinstance(result,dict) and result.get('csrf'):self.csrf=result['csrf']
                return r.status,result
        except urllib.error.HTTPError as e:
            return e.code,json.loads(e.read())
def upload_review(client, data):
    boundary='cielo'+uuid.uuid4().hex
    chunk=lambda kind,payload:struct.pack('!I',len(payload))+kind+payload+struct.pack('!I',zlib.crc32(kind+payload)&0xffffffff)
    png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!IIBBBBB',100,100,8,2,0,0,0))+chunk(b'IDAT',zlib.compress((b'\0'+b'\xd0\xb0\x90'*100)*100))+chunk(b'IEND',b'')
    payload=b''
    for key in ['productId','orderId','body']:
        payload+=f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{data[key]}\r\n'.encode()
    payload+=f'--{boundary}\r\nContent-Disposition: form-data; name="photo"; filename="test.png"\r\nContent-Type: image/png\r\n\r\n'.encode()+png+f'\r\n--{boundary}--\r\n'.encode()
    req=urllib.request.Request(URL+'/account/reviews',data=payload,method='POST',headers={'Content-Type':'multipart/form-data; boundary='+boundary,'X-CSRF-Token':client.csrf})
    try:
        with client.opener.open(req,timeout=20) as r:return r.status,json.load(r)
    except urllib.error.HTTPError as e:return e.code,json.loads(e.read())
def order(product,quantity=1,**extra):
    return {'requestId':str(uuid.uuid4()),'firstName':'Cliente','lastName':'Prueba','email':'prueba-cielo@example.test','phone':'987654321','documentType':'dni','documentId':'12345678','addressLine':'Av. Pruebas 123','reference':'','ubigeo':'150101','postal':'15001','paymentMethod':'yape','giftMessage':'Regalo de prueba','coupon':'','items':[{'productId':product,'quantity':quantity}],**extra}
public=Client();admin=Client()
setup=admin.call('POST','/auth/setup',{'firstName':'Admin','lastName':'Prueba','email':ADMIN_EMAIL,'phone':'','password':ADMIN_PASSWORD,'passwordConfirmation':ADMIN_PASSWORD})
if setup[0]!=200:assert admin.call('POST','/auth/login',{'email':ADMIN_EMAIL,'password':ADMIN_PASSWORD})[0]==200
check(public.call('GET','/health')[1]['ok'],'Conexión Java con PostgreSQL')
rows=public.call('GET','/products')[1]
check(len(rows)>=8,'Catálogo con al menos los 8 productos iniciales')
custom=next(p for p in rows if p['customizable']);simple=next(p for p in rows if p['reference']=='ANI-001')
original_stock=custom['stock']
assert public.call('POST','/auth/register',{'firstName':'Cliente','lastName':'Prueba','email':'prueba-cielo@example.test','password':'CieloCuenta2026Local'})[0]==200
data=order(custom['id'],coupon='CIELO10',items=[{'productId':custom['id'],'quantity':1,'engraving':'Ana','size':'6','price':0},{'productId':custom['id'],'quantity':1,'engraving':'Luz','size':'7','price':0}],total=0)
code,result=public.call('POST','/orders',data)
check(code==200, 'Creación de pedido: '+str(result))
pid=result['id'];expected=Decimal(str(custom['price']))*2*Decimal('.90')
check(Decimal(str(result['total']))==expected,'Precio y descuento calculados en el servidor')
check(public.call('POST','/orders',data)[1]['id']==pid,'Reintento del pedido sin duplicados')
detail=admin.call('GET',f'/admin/orders/{pid}')[1]
check([x['engraving'] for x in detail['items']]==['Ana','Luz'],'Dos grabados conservados por separado')
stock=lambda ident:next(p['stock'] for p in public.call('GET','/products')[1] if p['id']==ident)
check(stock(custom['id'])==original_stock,'Pedido pendiente conserva stock')
check(Decimal(str(admin.call('GET','/admin/report')[1]['summary']['revenue']))==0,'Ingresos excluyen pedidos pendientes')
check(public.call('POST','/orders',order(custom['id'],99))[0]==400,'Rechazo de stock insuficiente')
check(public.call('POST','/orders',order(custom['id'],-1))[0]==400,'Rechazo de cantidad negativa')
check(public.call('POST','/orders',order(custom['id'],ubigeo='999999'))[0]==400,'Validación de ubigeo en Java')
check(public.call('POST','/orders',order(custom['id'],documentId='123'))[0]==400,'Validación de documento en Java')
review={'productId':custom['id'],'orderId':pid,'email':data['email'],'name':'Cliente Prueba','body':'La joya llegó en perfecto estado.','photo':''}
check(upload_review(public,review)[0]==400,'Reseña requiere pago confirmado')
check(admin.call('PATCH',f'/admin/orders/{pid}/status',{'status':'pagado'})[0]==200,'Confirmación de pago')
check(stock(custom['id'])==original_stock-2,'Pago confirmado descuenta stock')
check(admin.call('PATCH',f'/admin/orders/{pid}/status',{'status':'pagado'})[0]==200 and stock(custom['id'])==original_stock-2,'Confirmar dos veces no vuelve a descontar')
check(Decimal(str(admin.call('GET','/admin/report')[1]['summary']['revenue']))==expected,'Reporte de ingresos confirmados')
check(upload_review(public,review)[0]==200,'Reseña de compra verificada')
check(public.call('GET',f"/reviews?productId={custom['id']}")[1]==[],'Reseña espera moderación')
rid=next(r['id'] for r in admin.call('GET','/admin/reviews')[1] if not r['demonstration'])
check(admin.call('PATCH',f'/admin/reviews/{rid}',{'approved':True})[0]==200 and len(public.call('GET',f"/reviews?productId={custom['id']}")[1])==1,'Moderación y publicación de reseña')
g={'name':'Cliente Prueba','email':data['email'],'product':custom['name'],'description':'Consulta por un defecto de fabricación.','evidence':'','orderId':pid}
check(public.call('POST','/guarantees',{**g,'email':'otra@example.test'})[0]==400,'Garantía comprueba correo de compra')
code,case=public.call('POST','/guarantees',g)
check(code==200 and admin.call('PATCH',f"/admin/guarantees/{case['caseId']}",{'status':'aprobada','note':'Revisar fotografía'})[0]==200,'Solicitud y seguimiento de garantía')
code,v=admin.call('POST','/admin/vendors',{'name':'Proveedor de prueba','contact':'test@example.test','material':'Acero','notes':'Prueba temporal','active':True})
check(code==200,'Registro de proveedor')
code,purchase=admin.call('POST','/admin/purchases',{'vendorId':v['id'],'productId':custom['id'],'quantity':3,'unitCost':'10.25','reference':'PRUEBA-001'})
check(code==200 and stock(custom['id'])==original_stock+1,'Compra a proveedor aumenta existencias')
check(public.call('POST','/newsletter',{'email':data['email']})[0]==200 and len(admin.call('GET','/admin/segments')[1])==1,'Suscripción integrada al cliente')
# Dos pedidos compiten por la última unidad del mismo producto.
simple['stock']=1
check(admin.call('POST','/admin/products',simple)[0]==200,'Edición de inventario')
a=public.call('POST','/orders',order(simple['id']))[1]['id'];b=public.call('POST','/orders',order(simple['id']))[1]['id']
def confirm(ident):
    operator=Client();assert operator.call('POST','/auth/login',{'email':ADMIN_EMAIL,'password':ADMIN_PASSWORD})[0]==200
    return operator.call('PATCH',f'/admin/orders/{ident}/status',{'status':'pagado'})
with ThreadPoolExecutor(max_workers=2) as pool: results=list(pool.map(confirm,[a,b]))
check(sorted(r[0] for r in results)==[200,409] and stock(simple['id'])==0,'Bloqueo concurrente evita sobreventa: '+str([r[0] for r in results]))
winner=[a,b][next(i for i,r in enumerate(results) if r[0]==200)]
check(admin.call('PATCH',f'/admin/orders/{winner}/status',{'status':'cancelado'})[0]==200 and stock(simple['id'])==1,'Cancelación repone existencias una vez')
check(admin.call('PATCH',f'/admin/orders/{winner}/status',{'status':'cancelado'})[0]==200 and stock(simple['id'])==1,'Cancelación repetida conserva stock')
# Probar rollback después de modificar una primera pieza y fallar en la segunda.
combo=order(simple['id'],items=[{'productId':simple['id'],'quantity':1},{'productId':custom['id'],'quantity':1}])
combo_id=public.call('POST','/orders',combo)[1]['id'];custom['stock']=0
check(admin.call('POST','/admin/products',custom)[0]==200,'Preparación de prueba de transacción')
check(admin.call('PATCH',f'/admin/orders/{combo_id}/status',{'status':'pagado'})[0]==409 and stock(simple['id'])==1,'Fallo de pago revierte todos los descuentos de stock')
check(admin.call('GET',f'/admin/orders/{pid}')[1]['total']==result['total'],'Pedido conserva el importe histórico')
print(json.dumps({'checks':len(checks),'result':'OK','database':'instancia temporal PostgreSQL','tests':checks},ensure_ascii=False,indent=2))
