"""Edición administrativa de reseñas. Solo contra PostgreSQL temporal autorizado."""
import os,json,uuid,urllib.request,urllib.error,http.cookiejar,struct,zlib
from pathlib import Path
assert os.environ.get('CIELO_TEST_ALLOW_MUTATIONS')=='1'
ROOT=os.environ.get('API_URL','http://127.0.0.1:18081');checks=[]
def check(ok,label):
 assert ok,label
 checks.append(label);print('OK:',label)
class Client:
 def __init__(self):
  self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()));self.csrf=None;self.csrf=self.call('GET','/auth/me')[1]['csrf']
 def request(self,method,path,data=None,headers=None):
  try:
   with self.opener.open(urllib.request.Request(ROOT+(path if path.startswith('/api/') else '/api'+path),method=method,data=data,headers=headers or {}),timeout=20) as r:
    raw=r.read();d=json.loads(raw) if 'application/json' in r.headers.get('Content-Type','') else raw
    if isinstance(d,dict) and 'csrf' in d:self.csrf=d['csrf']
    return r.status,d
  except urllib.error.HTTPError as e:
   raw=e.read()
   try:raw=json.loads(raw)
   except ValueError:pass
   return e.code,raw
 def call(self,method,path,data=None):return self.request(method,path,None if data is None else json.dumps(data).encode(),{'Content-Type':'application/json','X-CSRF-Token':self.csrf or ''})
 def edit(self,rid,body='Una experiencia bonita con esta pieza, que combina con todos mis accesorios.',photo=None,csrf=True,**extra):
  boundary='cielo'+uuid.uuid4().hex;payload=b''
  for k,v in {'body':body,**extra}.items():payload+=f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
  if photo is not None:payload+=f'--{boundary}\r\nContent-Disposition: form-data; name="photo"; filename="nueva.png"\r\nContent-Type: image/png\r\n\r\n'.encode()+photo+b'\r\n'
  payload+=f'--{boundary}--\r\n'.encode()
  return self.request('PUT',f'/admin/reviews/{rid}',payload,{'Content-Type':'multipart/form-data; boundary='+boundary,'X-CSRF-Token':self.csrf if csrf else ''})
def png():
 def chunk(k,d):return struct.pack('!I',len(d))+k+d+struct.pack('!I',zlib.crc32(k+d)&0xffffffff)
 return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!IIBBBBB',120,120,8,2,0,0,0))+chunk(b'IDAT',zlib.compress((b'\0'+b'\xc5\xb0\x91'*120)*120))+chunk(b'IEND',b'')
admin=Client();guest=Client();client=Client()
assert admin.call('POST','/auth/login',{'email':'admin-prueba@example.test','password':'CieloPrueba2026Local'})[0]==200
assert client.call('POST','/auth/register',{'firstName':'Clienta','lastName':'Edición','email':uuid.uuid4().hex+'@example.test','password':'CieloPrueba2026Local'})[0]==200
rows=admin.call('GET','/admin/reviews')[1];demo=next(r for r in rows if r['demonstration']);rid=demo['id']
def actual():return next(r for r in admin.call('GET','/admin/reviews')[1] if r['id']==rid)
check(guest.edit(rid)[0]==401,'Visitante no edita reseñas')
check(client.edit(rid)[0]==403,'Cliente no edita desde el panel administrativo')
check(admin.edit(rid,csrf=False)[0]==403,'Edición exige CSRF')
check(admin.edit(999999999)[0]==404,'Reseña inexistente devuelve 404')
check(admin.edit(rid,body='corto')[0]==400,'Texto demasiado corto rechazado')
check(admin.edit(rid,body='x'*3001)[0]==400,'Texto de más de 3000 caracteres rechazado')
check(actual()==demo,'Errores de validación conservan la reseña original')
check(admin.edit(rid,photo=b'archivo falso')[0]==400,'Imagen falsa rechazada')
check(admin.edit(rid,photo=b'')[0]==400,'Archivo de imagen vacío rechazado')
check(admin.edit(rid,photo=b'x'*(5*1024*1024+1))[0]==413,'Imagen mayor de 5 MB rechazada')
check(actual()==demo,'Una foto inválida tampoco modifica el texto')
check(admin.edit(rid)[0]==200,'Admin actualiza solo el texto')
a=actual();check(a['photo']==demo['photo'] and a['body']!=demo['body'],'Se conserva la imagen si no se elige otra')
check(all(a[k]==demo[k] for k in ['name','email','product','verified','demonstration','approved']),'Se conservan autoría, producto, verificación y publicación')
check(admin.edit(rid,photo=png(),name='Otra autora',productId=999,approved='false',verified='true')[0]==200,'Admin puede reemplazar la fotografía')
a=actual();check(a['photo'].startswith('/api/review-photos/') and a['photo']!=demo['photo'],'Nueva foto guardada con ruta controlada por Java')
check(all(a[k]==demo[k] for k in ['name','product','verified','demonstration','approved']),'Campos extra no cambian compra, identidad ni publicación')
check(guest.call('GET',a['photo'])[1][:2]==b'\xff\xd8','Foto de reseña publicada accesible como JPEG normalizado')
public=next(r for r in guest.call('GET','/reviews')[1] if r['id']==rid)
check(public['body']==a['body'] and public['photo']==a['photo'] and not public['verified'],'Cambios visibles sin convertir un ejemplo en compra verificada')
old=a['photo'];check(admin.edit(rid,photo=png())[0]==200,'Segunda sustitución de foto correcta')
check(guest.call('GET',old)[0]==404,'Foto reemplazada deja de ser accesible')
check(not Path('/private/tmp/cielo-editar-resenas-fotos',old.split('/')[-1]).exists(),'Archivo reemplazado eliminado después de guardar')
assert admin.call('PATCH',f'/admin/reviews/{rid}',{'approved':False})[0]==200
check(admin.edit(rid,body='Texto editado mientras la reseña continúa oculta.')[0]==200 and not actual()['approved'],'Editar no publica una reseña oculta')
photo=actual()['photo'];check(guest.call('GET',photo)[0]==404 and admin.call('GET',photo)[0]==200,'La foto oculta sigue reservada a administración')
assert admin.call('PATCH',f'/admin/reviews/{rid}',{'approved':True})[0]==200
print(json.dumps({'checks':len(checks),'result':'OK'},ensure_ascii=False))
