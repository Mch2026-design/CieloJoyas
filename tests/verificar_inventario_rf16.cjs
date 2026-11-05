'use strict';
// Interfaz con API simulada: nunca altera proveedores ni compras reales.
const {chromium,webkit}=require('/Users/flavio/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs=require('node:fs');const assert=require('node:assert/strict');
const base=process.env.API_URL||'http://127.0.0.1:18085';
(async()=>{
 for(const engine of [chromium,webkit]){
  const browser=await engine.launch(engine===chromium?{executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true}:{headless:true});
  const page=await browser.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
  let rows=[{id:1,nombre:'Acero <seguro>',contacto:'acero@example.test',material:'316L',notas:'Ejemplo',activo:true,categoriaInsumo:'acero'}],fail=false;
  await page.route('**/admin-inventario.html',r=>r.fulfill({contentType:'text/html',body:fs.readFileSync('src/main/resources/static/admin-inventario.html','utf8')}));
  await page.route('**/api/auth/me',r=>r.fulfill({json:{user:{role:'ADMIN',name:'Prueba'},csrf:'test'}}));
  await page.route('**/api/admin/vendors',async r=>{
   if(r.request().method()==='GET')return r.fulfill({json:rows});
   if(fail)return r.fulfill({status:400,json:{error:'Revisa los datos del proveedor.'}});
   const b=r.request().postDataJSON(),row={id:Number(b.id)||2,nombre:b.name,contacto:b.contact,material:b.material,notas:b.notes,activo:b.active,categoriaInsumo:b.supplyCategory};
   rows=rows.filter(x=>x.id!==row.id).concat(row);return r.fulfill({json:row});
  });
  await page.goto(base+'/admin-inventario.html');await page.locator('.supply-section').last().waitFor();
  assert.equal(await page.locator('.supply-section').count(),4);
  assert.equal(await page.locator('.supply-section').first().locator('b').textContent(),'Acero <seguro>');
  await page.getByRole('button',{name:'Editar proveedor Acero <seguro>',exact:true}).click();
  await page.locator('[name=name]').fill('Acero actualizado');await page.locator('[name=active]').uncheck();
  await page.getByRole('button',{name:'Guardar proveedor',exact:true}).click();await page.getByText('Acero actualizado',{exact:true}).waitFor();
  assert.equal(rows[0].activo,false);assert.equal(rows[0].categoriaInsumo,'acero');
  await page.locator('[data-new-vendor=gemas]').click();assert.equal(await page.locator('[name=id]').inputValue(),'');
  assert.equal(await page.locator('[name=supplyCategory]').inputValue(),'gemas');
  await page.locator('[name=name]').fill('Gemas de prueba');fail=true;
  await page.getByRole('button',{name:'Guardar proveedor',exact:true}).click();await page.locator('#vendor-error').filter({hasText:'Revisa'}).waitFor();
  assert.equal(await page.locator('[name=name]').inputValue(),'Gemas de prueba');
  fail=false;await page.getByRole('button',{name:'Guardar proveedor',exact:true}).click();await page.getByText('Gemas de prueba',{exact:true}).waitFor();
  assert.equal(rows.find(x=>x.id===2).categoriaInsumo,'gemas');
  for(const size of [{width:1440,height:1000},{width:375,height:812},{width:812,height:375}]){
   await page.setViewportSize(size);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Desbordamiento horizontal');
  }
  await page.emulateMedia({reducedMotion:'reduce'});assert.deepEqual(errors,[]);
  fs.mkdirSync('verificacion',{recursive:true});await page.screenshot({path:'verificacion/inventario-rf16-'+engine.name()+'.png',fullPage:true});
  await browser.close();console.log('OK '+engine.name()+': cuatro apartados, edición, alta, error recuperable y responsive');
 }
})().catch(e=>{console.error(e);process.exit(1);});
