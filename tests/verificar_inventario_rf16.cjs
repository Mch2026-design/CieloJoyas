'use strict';
// Interfaz con API simulada: nunca altera proveedores ni compras reales.
const {chromium,webkit}=require('/Users/flavio/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs=require('node:fs');const assert=require('node:assert/strict');
const base=process.env.API_URL||'http://127.0.0.1:18085';
(async()=>{
 for(const engine of [chromium,webkit]){
  const browser=await engine.launch(engine===chromium?{executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true}:{headless:true});
  const page=await browser.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
  const imageUrl='https://imagenes.example.test/proveedor.svg';
  await page.route('https://imagenes.example.test/**',r=>r.request().url().includes('error')?r.fulfill({status:404}):r.fulfill({contentType:'image/svg+xml',body:'<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80"><rect width="80" height="80" fill="#edd9c8"/><text x="20" y="50" fill="#49372c" font-size="28">CJ</text></svg>'}));
  let rows=[{id:1,nombre:'Acero <seguro>',contacto:'acero@example.test',material:'316L',notas:'Ejemplo',activo:true,categoriaInsumo:'acero'}],fail=false;
  await page.route('**/admin-inventario.html',r=>r.fulfill({contentType:'text/html',body:fs.readFileSync('src/main/resources/static/admin-inventario.html','utf8')}));
  await page.route('**/api/auth/me',r=>r.fulfill({json:{user:{role:'ADMIN',name:'Prueba'},csrf:'test'}}));
  await page.route('**/api/admin/vendors',async r=>{
   if(r.request().method()==='GET')return r.fulfill({json:rows});
   if(fail)return r.fulfill({status:400,json:{error:'Revisa los datos del proveedor.'}});
   const b=r.request().postDataJSON(),row={id:Number(b.id)||2,nombre:b.name,contacto:b.contact,material:b.material,notas:b.notes,activo:b.active,categoriaInsumo:b.supplyCategory,imagenUrl:b.imageUrl};
   rows=rows.filter(x=>x.id!==row.id).concat(row);return r.fulfill({json:row});
  });
  await page.goto(base+'/admin-inventario.html');await page.locator('.supply-section').last().waitFor();
  assert.equal(await page.locator('.supply-section').count(),4);
  assert.equal(await page.locator('.supply-section').first().locator('b').textContent(),'Acero <seguro>');
  await page.getByRole('button',{name:'Editar proveedor Acero <seguro>',exact:true}).click();
  await page.locator('[name=imageUrl]').fill(imageUrl);await page.locator('[name=imageUrl]').blur();await page.locator('#vendor-preview-image').waitFor({state:'visible'});
  await page.locator('[name=name]').fill('Acero actualizado');await page.locator('[name=active]').uncheck();
  await page.getByRole('button',{name:'Guardar proveedor',exact:true}).click();await page.getByText('Acero actualizado',{exact:true}).waitFor();
  assert.equal(rows[0].imagenUrl,imageUrl);await page.locator('[data-vendor-image]').waitFor();assert.equal(await page.locator('[data-vendor-image]').count(),1);
  await page.getByRole('button',{name:'Editar proveedor Acero actualizado',exact:true}).click();assert.equal(await page.locator('[name=imageUrl]').inputValue(),imageUrl);
  await page.locator('[name=imageUrl]').fill('https://imagenes.example.test/error.png');await page.locator('[name=imageUrl]').blur();await page.getByText('No se pudo cargar la imagen. Revisa el enlace o prueba con otro.',{exact:true}).waitFor();
  assert(await page.locator('#vendor-preview-image').isHidden());
  await page.locator('[name=imageUrl]').fill('');await page.locator('[name=imageUrl]').blur();await page.getByRole('button',{name:'Guardar proveedor',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('[data-vendor-image]'));
  assert.equal(rows[0].imagenUrl,'');
  assert.equal(rows[0].activo,false);assert.equal(rows[0].categoriaInsumo,'acero');
  await page.locator('[data-new-vendor=gemas]').click();assert.equal(await page.locator('[name=id]').inputValue(),'');assert.equal(await page.locator('[name=imageUrl]').inputValue(),'');
  assert.equal(await page.locator('[name=supplyCategory]').inputValue(),'gemas');
  await page.locator('[name=name]').fill('Gemas de prueba');await page.locator('[name=imageUrl]').fill(imageUrl);await page.locator('[name=imageUrl]').blur();fail=true;
  await page.getByRole('button',{name:'Guardar proveedor',exact:true}).click();await page.locator('#vendor-error').filter({hasText:'Revisa'}).waitFor();
  assert.equal(await page.locator('[name=name]').inputValue(),'Gemas de prueba');assert.equal(await page.locator('[name=imageUrl]').inputValue(),imageUrl);
  fail=false;await page.getByRole('button',{name:'Guardar proveedor',exact:true}).click();await page.getByText('Gemas de prueba',{exact:true}).waitFor();
  assert.equal(rows.find(x=>x.id===2).categoriaInsumo,'gemas');
  for(const size of [{width:1440,height:1000},{width:375,height:812},{width:812,height:375}]){
   await page.setViewportSize(size);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Desbordamiento horizontal');
  }
  await page.setViewportSize({width:1440,height:1000});await page.evaluate(()=>scrollTo(0,0));
  await page.emulateMedia({reducedMotion:'reduce'});assert.deepEqual(errors,[]);
  fs.mkdirSync('verificacion',{recursive:true});await page.screenshot({path:'verificacion/inventario-rf16-'+engine.name()+'.png',fullPage:true});
  await browser.close();console.log('OK '+engine.name()+': cuatro apartados, edición, alta, imagen, vista previa, eliminación, enlace roto y responsive');
 }
})().catch(e=>{console.error(e);process.exit(1);});
