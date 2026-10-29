"""Comprobaciones de cuentas y roles. Solo para la base temporal de pruebas."""
import os,json,uuid,urllib.request,urllib.error,http.cookiejar
assert os.environ.get('CIELO_TEST_ALLOW_MUTATIONS')=='1','Autorizar solo contra una base temporal.'
ROOT=os.environ.get('API_URL','http://127.0.0.1:18081');URL=ROOT+'/api'
checks=[]
def check(ok,label):
    assert ok,label
    checks.append(label);print('OK:',label)
class Client:
    def __init__(self):
        self.jar=http.cookiejar.CookieJar();self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar));self.csrf=None
        self.csrf=self.call('GET','/auth/me')[1]['csrf']
    def call(self,method,path,data=None,csrf=True):
        headers={'Accept':'application/json'}
        if data is not None:headers['Content-Type']='application/json'
        if csrf and self.csrf and method!='GET':headers['X-CSRF-Token']=self.csrf
        req=urllib.request.Request(URL+path,method=method,headers=headers,data=json.dumps(data).encode() if data is not None else None)
        try:
            with self.opener.open(req,timeout=15) as r:
                self.headers=r.headers;d=json.load(r)
                if isinstance(d,dict) and d.get('csrf'):self.csrf=d['csrf']
                return r.status,d
        except urllib.error.HTTPError as e:return e.code,json.loads(e.read())
    def sid(self):return next((c.value for c in self.jar if c.name=='JSESSIONID'),None)
    def page(self,path):
        with self.opener.open(ROOT+'/'+path,timeout=15) as response:
            return response.geturl(),response.read().decode()
def signup(email,first='Cliente',**extra):return {'firstName':first,'lastName':'Prueba','email':email,'password':'CieloCuenta2026Local',**extra}
public=Client();admin=Client();email='admin-prueba@example.test';password='CieloPrueba2026Local'
check(public.call('GET','/admin/products')[0]==401,'Visitante no accede a productos administrativos')
check(public.call('GET','/admin/report')[0]==401,'Visitante no accede a reportes')
check(public.call('POST','/auth/login',{'email':email,'password':password},csrf=False)[0]==403,'Formulario requiere el token de su sesión')
check(admin.call('POST','/auth/login',{'email':email,'password':'incorrecta'})[0]==401,'Contraseña incorrecta rechazada')
before=admin.sid();code,data=admin.call('POST','/auth/login',{'email':email,'password':password})
check(code==200 and data['user']['role']=='ADMIN','Acceso administrativo')
check(admin.sid()!=before,'Identificador de sesión renovado al entrar')
check(any(c.has_nonstandard_attr('HttpOnly') for c in admin.jar),'Cookie de sesión HttpOnly')
check('password' not in json.dumps(data).lower(),'Respuesta de acceso no expone contraseñas')
check(public.call('POST','/auth/setup',signup('segundo-admin@example.test'))[0]==409,'Primer administrador no puede configurarse dos veces')
a=Client();b=Client()
code,created=a.call('POST','/auth/register',signup('cuenta-a@example.test',role='ADMIN'))
check(code==200 and created['user']['role']=='CLIENTE','Registro sin celular ni repetición de contraseña crea solo CLIENTE')
check(b.call('POST','/auth/register',signup('corta@example.test',password='corta'))[0]==400,'Registro conserva el mínimo de 8 caracteres')
check(b.call('POST','/auth/register',signup('CUENTA-A@example.test'))[0]==409,'Correo duplicado sin diferencia de mayúsculas')
check(b.call('POST','/auth/register',signup('cuenta-b@example.test',first='Otra'))[0]==200,'Segunda cuenta de cliente')
pages=['productos.html','admin-pedidos.html','admin-reportes.html','admin-compras.html','admin-posventa.html','admin-promociones.html']
for page in pages:
    check(public.page(page)[0].split('?')[0].endswith('/login.html'),'Visitante requiere acceso: '+page)
    check(a.page(page)[0].endswith('/index.html'),'Cliente vuelve al inicio: '+page)
    url,html=admin.page(page)
    check(url.endswith('/'+page) and 'aria-current="page"' in html,'Administrador abre el documento: '+page)
check(admin.page('admin.html')[0].endswith('/productos.html'),'Entrada antigua del panel redirige a Productos')
check(admin.page('admin-productos.html')[0].endswith('/productos.html'),'Enlace anterior de productos sigue funcionando')
check(a.call('GET','/admin/report')[0]==403,'Cliente no consulta reportes administrativos')
check(a.call('POST','/admin/products',{})[0]==403,'Cliente no agrega ni edita productos')
check(a.call('DELETE','/admin/products/1')[0]==403,'Cliente no elimina productos')
check(a.call('GET','/account/orders')[1]==[],'Cuenta nueva no hereda pedidos de otros clientes')
products=public.call('GET','/products')[1];product=next(p for p in products if p['stock']>=3)
def order():return {'requestId':str(uuid.uuid4()),'firstName':'Cliente','lastName':'Cuenta','email':'cuenta-b@example.test','phone':'987654321','documentType':'dni','documentId':'12345678','addressLine':'Av. Pruebas 456','reference':'','ubigeo':'150101','postal':'','paymentMethod':'plin','coupon':'','giftMessage':'','items':[{'productId':product['id'],'quantity':1}]}
code,purchase=a.call('POST','/orders',order())
check(code==200,'Cliente registra su pedido')
own=a.call('GET','/account/orders')[1]
check(len(own)==1 and own[0]['id']==purchase['id'],'Historial contiene el pedido del usuario autenticado')
check(own[0]['email']=='cuenta-a@example.test','Correo de compra procede de la sesión')
check(b.call('GET','/account/orders')[1]==[],'Otro cliente no ve el pedido ajeno')
check(a.call('GET',f"/admin/orders/{purchase['id']}")[0]==403,'Cliente no accede al detalle administrativo ajeno')
start=admin.call('GET','/admin/report')[1]
check(admin.call('PATCH',f"/admin/orders/{purchase['id']}/status",{'status':'pagado'})[0]==200,'Administrador confirma el pago del cliente')
end=admin.call('GET','/admin/report')[1]
check(round(float(end['summary']['revenue'])-float(start['summary']['revenue']),2)==round(float(purchase['total']),2),'Reporte incorpora únicamente la venta confirmada')
check(any(x['method']=='plin' for x in end['byPayment']),'Estadística de ventas por medio de pago')
check(sum(x['count'] for x in end['byStatus'])==end['summary']['orders'],'Distribución de estados coincide con pedidos')
# CRUD completo de una pieza creada exclusivamente en esta prueba.
p={**product,'id':'','reference':'TEST-'+uuid.uuid4().hex[:12],'name':'Pieza de prueba temporal','stock':3,'price':25,'compareAtPrice':'','description':'Solo en la base temporal.','active':True}
code,added=admin.call('POST','/admin/products',p)
check(code==200,'Administrador crea un producto')
pid=added['id'];added.update(name='Pieza de prueba editada',price=29)
check(admin.call('POST','/admin/products',added)[0]==200,'Administrador edita el producto')
check(any(x['id']==pid and x['name']==added['name'] for x in public.call('GET','/products')[1]),'Catálogo refleja la edición')
check(admin.call('DELETE',f'/admin/products/{pid}')[0]==200,'Administrador elimina el producto del catálogo')
check(not any(x['id']==pid for x in public.call('GET','/products')[1]),'Producto eliminado desaparece de la tienda')
check(any(x['id']==pid and not x['active'] for x in admin.call('GET','/admin/products')[1]),'Baja conserva referencias históricas')
check(a.call('POST','/auth/logout',{})[0]==200 and a.call('GET','/account/orders')[0]==401,'Cerrar sesión retira el acceso a pedidos')
print(json.dumps({'result':'OK','checks':len(checks),'tests':checks},ensure_ascii=False,indent=2))
