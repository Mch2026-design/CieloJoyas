"""Guardado opcional por cuenta. Ejecutar solo en la base temporal autorizada."""
import os,json,uuid,urllib.request,urllib.error,http.cookiejar
assert os.environ.get('CIELO_TEST_ALLOW_MUTATIONS')=='1'
ROOT=os.environ.get('API_URL','http://127.0.0.1:18081');checks=[]
def check(ok,label):
 assert ok,label
 checks.append(label);print('OK:',label)
class Client:
 def __init__(self):
  self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()));self.csrf=None;self.csrf=self.call('GET','/auth/me')[1]['csrf']
 def call(self,method,path,data=None,csrf=True):
  req=urllib.request.Request(ROOT+'/api'+path,method=method,data=None if data is None else json.dumps(data).encode(),headers={'Content-Type':'application/json','X-CSRF-Token':self.csrf if csrf and self.csrf else ''})
  try:
   with self.opener.open(req,timeout=20) as r:
    d=json.load(r)
    if isinstance(d,dict) and 'csrf' in d:self.csrf=d['csrf']
    return r.status,d
  except urllib.error.HTTPError as e:return e.code,json.loads(e.read())
guest=Client();a=Client();b=Client()
def register(client,name):
 email=uuid.uuid4().hex+'@example.test';password='CieloPrueba2026Local'
 assert client.call('POST','/auth/register',{'firstName':name,'lastName':'Recordar','email':email,'password':password})[0]==200
 return email,password
authA=register(a,'Ana');authB=register(b,'Beatriz')
products=a.call('GET','/products')[1];p=next(p for p in products if p['stock']>3)
def order(**extra):return {'requestId':str(uuid.uuid4()),'firstName':'Ana','lastName':'Recordar','email':authA[0],'phone':'987654321','documentType':'dni','documentId':'12345678','addressLine':'Av. Guardada 123','reference':'Piso 2','ubigeo':'150101','postal':'15001','paymentMethod':'yape','items':[{'productId':p['id'],'quantity':1}],**extra}
check(guest.call('GET','/account/delivery')[0]==401,'Visitante no consulta datos guardados')
check(a.call('GET','/account/delivery')[1]=={'saved':False},'Cuenta nueva no tiene datos de otra cuenta')
check(guest.call('POST','/orders',order(rememberDelivery=True))[0]==400,'Visitante no guarda datos sin cuenta')
check(a.call('POST','/orders',order(rememberDelivery=True,documentId='x'))[0]==400,'Compra inválida rechazada')
check(a.call('GET','/account/delivery')[1]=={'saved':False},'Una compra fallida no guarda datos')
check(a.call('POST','/orders',order())[0]==200,'Compra sin marcar permitida')
check(not a.call('GET','/account/delivery')[1]['saved'],'Sin consentimiento no se guarda la entrega')
data=order(rememberDelivery=True);check(a.call('POST','/orders',data)[0]==200,'Compra con Recuérdame registrada')
saved=a.call('GET','/account/delivery')[1]
check(saved['saved'] and saved['address']=='Av. Guardada 123' and saved['documentId']=='12345678' and saved['district']=='150101' and saved['phone']=='987654321','Datos completos recordados después de comprar')
check('paymentMethod' not in saved and 'items' not in saved,'No se guardan medios de pago ni selección del carrito como preferencia')
check(not b.call('GET','/account/delivery')[1]['saved'],'Otra cuenta no recibe la información de Ana')
check(a.call('POST','/orders',data)[0]==200 and a.call('GET','/account/delivery')[1]==saved,'Reintento conserva datos sin duplicar guardado')
check(a.call('POST','/orders',order(addressLine='Otra dirección 456'))[0]==200 and a.call('GET','/account/delivery')[1]==saved,'Compra sin marcar no sobrescribe los datos anteriores')
check(a.call('POST','/orders',order(rememberDelivery=True,addressLine='Nueva dirección 789'))[0]==200 and a.call('GET','/account/delivery')[1]['address']=='Nueva dirección 789','Marcar de nuevo actualiza la entrega guardada')
check(b.call('POST','/orders',order(rememberDelivery=True,firstName='Beatriz',addressLine='Calle Beatriz 456',documentId='87654321'))[0]==200,'Segunda cuenta guarda su propia entrega')
check(a.call('GET','/account/delivery')[1]['address']!=b.call('GET','/account/delivery')[1]['address'],'Datos distintos aislados por cuenta')
check(a.call('POST','/auth/logout')[0]==200 and a.call('GET','/account/delivery')[0]==401,'Cerrar sesión protege los datos recordados')
a.csrf=a.call('GET','/auth/me')[1]['csrf']
check(a.call('POST','/auth/login',{'email':authA[0],'password':authA[1]})[0]==200 and a.call('GET','/account/delivery')[1]['address']=='Nueva dirección 789','Datos recuperados al iniciar sesión otra vez')
check(a.call('DELETE','/account/delivery',csrf=False)[0]==403,'Olvidar datos requiere CSRF')
check(a.call('DELETE','/account/delivery')[0]==200 and not a.call('GET','/account/delivery')[1]['saved'],'Olvidar datos elimina la preferencia')
check(b.call('GET','/account/delivery')[1]['saved'],'Olvidar una cuenta no altera la otra')
print(json.dumps({'checks':len(checks),'result':'OK'},ensure_ascii=False))
