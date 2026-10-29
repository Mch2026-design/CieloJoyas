"""Pruebas reales de reseñas. Solo ejecutar en PostgreSQL temporal autorizado."""
import os,json,uuid,urllib.request,urllib.error,http.cookiejar,struct,zlib
assert os.environ.get('CIELO_TEST_ALLOW_MUTATIONS')=='1','Usar solo base temporal.'
ROOT=os.environ.get('API_URL','http://127.0.0.1:18081');checks=[]
def check(ok,label):
 assert ok,label
 checks.append(label);print('OK:',label)
class Client:
 def __init__(self):
  self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()));self.csrf=None;self.csrf=self.call('GET','/api/auth/me')[1]['csrf']
 def request(self,method,path,data=None,headers=None):
  req=urllib.request.Request(ROOT+path,method=method,data=data,headers=headers or {})
  try:
   with self.opener.open(req,timeout=30) as r:
    raw=r.read();result=json.loads(raw) if 'application/json' in r.headers.get('Content-Type','') else raw
    if isinstance(result,dict) and result.get('csrf'):self.csrf=result['csrf']
    return r.status,result
  except urllib.error.HTTPError as e:
   raw=e.read()
   try:raw=json.loads(raw)
   except ValueError:pass
   return e.code,raw
 def call(self,method,path,data=None):
  return self.request(method,path,None if data is None else json.dumps(data).encode(),{'Content-Type':'application/json','X-CSRF-Token':self.csrf or ''})
 def upload(self,oid,pid,body='Una pieza preciosa, perfecta para acompañar mis días.',photo=None,csrf=True,**extras):
  boundary='cielo'+uuid.uuid4().hex;data=b''
  for k,v in {'orderId':oid,'productId':pid,'body':body,**extras}.items():data+=f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
  data+=f'--{boundary}\r\nContent-Disposition: form-data; name="photo"; filename="foto.png"\r\nContent-Type: image/png\r\n\r\n'.encode()+(png() if photo is None else photo)+f'\r\n--{boundary}--\r\n'.encode()
  return self.request('POST','/api/account/reviews',data,{'Content-Type':'multipart/form-data; boundary='+boundary,'X-CSRF-Token':self.csrf if csrf else ''})
def png(w=120,h=120):
 def chunk(k,d):return struct.pack('!I',len(d))+k+d+struct.pack('!I',zlib.crc32(k+d)&0xffffffff)
 return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!IIBBBBB',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress((b'\0'+b'\xc5\xb0\x91'*w)*h))+chunk(b'IEND',b'')
guest=Client();owner=Client();other=Client();admin=Client()
assert admin.call('POST','/api/auth/login',{'email':'admin-prueba@example.test','password':'CieloPrueba2026Local'})[0]==200
for client,name in [(owner,'Compradora'),(other,'Otra')]:
 check(client.call('POST','/api/auth/register',{'firstName':name,'lastName':'Reseñas','email':uuid.uuid4().hex+'@example.test','password':'CieloPrueba2026Local'})[0]==200,'Registro '+name)
feed=guest.call('GET','/api/reviews')[1];demos=[r for r in feed if r['demonstration']]
check(len(demos)==5 and all(not r['verified'] and r['photo'] and r['product']['name'] for r in demos),'Cinco ejemplos con foto y producto, separados de las compras verificadas')
check(guest.call('GET','/api/account/reviewable-products')[0]==401,'Visitante no consulta compras para reseñar')
check(owner.call('GET','/api/account/reviewable-products')[1]==[],'Cuenta nueva sin compras elegibles')
products=owner.call('GET','/api/products')[1];p=next(x for x in products if x['stock']>=3);q=next(x for x in products if x['id']!=p['id'])
body={'requestId':str(uuid.uuid4()),'firstName':'Compradora','lastName':'Reseñas','email':'ignorado@example.test','phone':'987654321','documentType':'dni','documentId':'12345678','addressLine':'Av. Pruebas 123','ubigeo':'150101','paymentMethod':'yape','items':[{'productId':p['id'],'quantity':1}]}
code,order=owner.call('POST','/api/orders',body);check(code==200,'Compra propia para probar reseña');oid=order['id']
check(guest.upload(oid,p['id'])[0]==401,'Visitante no publica con un número de pedido conocido')
check(owner.upload(oid,p['id'])[0]==400,'Pedido pendiente no permite reseña')
check(owner.call('GET','/api/account/reviewable-products')[1]==[],'Pedido pendiente no aparece en el selector')
check(admin.call('PATCH',f'/api/admin/orders/{oid}/status',{'status':'pagado'})[0]==200,'Pago confirmado habilita la pieza')
options=owner.call('GET','/api/account/reviewable-products')[1]
check(len(options)==1 and options[0]['productId']==p['id'] and options[0]['orderId']==oid,'Selector muestra solo la compra propia pagada')
check(other.call('GET','/api/account/reviewable-products')[1]==[],'Otra cuenta no hereda piezas para reseñar')
check(other.upload(oid,p['id'])[0]==400,'Pedido ajeno rechazado aunque conozca su identificador')
check(admin.upload(oid,p['id'])[0]==403,'Admin no se hace pasar por compradora')
check(owner.upload(oid,q['id'])[0]==400,'Producto ajeno al pedido rechazado')
check(owner.upload(oid,p['id'],csrf=False)[0]==403,'Carga protegida contra envíos sin CSRF')
check(owner.upload(oid,p['id'],photo=b'no es una imagen')[0]==400,'Archivo falso rechazado aunque declare PNG')
check(owner.upload(oid,p['id'],photo=b'')[0]==400,'Fotografía obligatoria')
check(owner.upload(oid,p['id'],photo=png(20,20))[0]==400,'Se rechaza foto demasiado pequeña')
check(owner.upload(oid,p['id'],photo=b'x'*(5*1024*1024+1))[0]==413,'Foto de más de 5 MB rechazada')
check(owner.upload(oid,p['id'],body='corto')[0]==400,'Reseña demasiado corta rechazada')
check(owner.upload(oid,p['id'],body='x'*3001)[0]==400,'Reseña de más de 3000 caracteres rechazada')
code,result=owner.upload(oid,p['id'],name='Nombre falsificado',verified=False)
check(code==200,'Carga real de foto PNG y reseña para moderación');rid=result['id']
check(owner.call('GET','/api/account/reviewable-products')[1]==[],'La pieza reseñada deja de ofrecerse')
check(owner.upload(oid,p['id'])[0]==400,'Reseña duplicada rechazada')
check(not any(x['id']==rid for x in guest.call('GET','/api/reviews')[1]),'Reseña sin moderar no se publica')
r=next(x for x in admin.call('GET','/api/admin/reviews')[1] if x['id']==rid);photo=r['photo']
check(r['name']=='Compradora Reseñas' and r['verified'] and not r['demonstration'],'Nombre y verificación provienen de sesión y compra')
check(guest.call('GET',photo)[0]==404 and other.call('GET',photo)[0]==404,'Foto pendiente no es pública ni visible para otra cuenta')
check(owner.call('GET',photo)[1][:2]==b'\xff\xd8' and admin.call('GET',photo)[0]==200,'Foto normalizada a JPEG visible para propietaria y moderación')
check(admin.call('PATCH',f'/api/admin/reviews/{rid}',{'approved':True})[0]==200,'Admin publica la reseña')
published=next(x for x in guest.call('GET','/api/reviews')[1] if x['id']==rid)
check(published['verified'] and published['product']['id']==p['id'] and published['photo']==photo,'Reseña publicada con producto, foto y sello verificado')
check(guest.call('GET',photo)[0]==200,'Foto aprobada visible públicamente')
check(all(x['product']['id']==p['id'] for x in guest.call('GET',f"/api/reviews?productId={p['id']}")[1]),'Filtro de producto conserva solo su pieza')
check(admin.call('PATCH',f'/api/admin/reviews/{rid}',{'approved':False})[0]==200 and guest.call('GET',photo)[0]==404,'Ocultar una reseña también protege su foto')
assert admin.call('PATCH',f'/api/admin/reviews/{rid}',{'approved':True})[0]==200
check(admin.call('PATCH',f'/api/admin/orders/{oid}/status',{'status':'cancelado'})[0]==200,'Cancelación del pedido aplicada')
check(not any(x['id']==rid for x in guest.call('GET','/api/reviews')[1]) and guest.call('GET',photo)[0]==404,'Un pago cancelado retira la reseña verificada y su foto')
check(admin.call('PATCH',f'/api/admin/reviews/{rid}',{'approved':True})[0]==400,'No se puede republicar una compra cancelada')
check(admin.call('PATCH',f"/api/admin/reviews/{demos[0]['id']}",{'approved':True})[0]==200,'Moderación de demostración funciona sin inventar pedidos')
print(json.dumps({'checks':len(checks),'result':'OK'},ensure_ascii=False))
