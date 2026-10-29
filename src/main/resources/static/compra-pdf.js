'use strict';
window.CieloPDF=(()=>{
  let library;
  function loadLibrary(){
    if(window.PDFLib)return Promise.resolve(window.PDFLib);
    if(!library)library=new Promise((resolve,reject)=>{const script=document.createElement('script');script.src='vendor/pdf-lib-1.17.1.min.js';script.onload=()=>resolve(window.PDFLib);script.onerror=()=>{script.remove();library=null;reject(Error('No se pudo preparar el PDF. Comprueba tu conexión y vuelve a intentarlo.'));};document.head.append(script);});
    return library;
  }
  async function buildOrder(order){
    const {PDFDocument,StandardFonts,rgb}=await loadLibrary();
    const doc=await PDFDocument.create();doc.setTitle(`Pedido ${order.id} - Cielo Joyas`);doc.setAuthor('Cielo Joyas');doc.setSubject('Información de compra y certificado de autenticidad y garantía');
    const font=await doc.embedFont(StandardFonts.Helvetica),bold=await doc.embedFont(StandardFonts.HelveticaBold),serif=await doc.embedFont(StandardFonts.TimesRoman);
    const charset=new Set(font.getCharacterSet());
    const clean=value=>Array.from(String(value??'').replace(/[\r\t]/g,' ')).map(c=>c==='\n'||charset.has(c.codePointAt(0))?c:'?').join('');
    const ink=rgb(.145,.133,.122),muted=rgb(.38,.35,.32),accent=rgb(.5,.325,.247),line=rgb(.89,.87,.84);
    let page,y;const left=48,right=547,width=499;
    const write=(value,x,pos,size=11,f=font,color=ink)=>page.drawText(clean(value),{x,y:pos,size,font:f,color});
    const rightText=(value,pos,size=11,f=font)=>{const text=clean(value);write(text,right-f.widthOfTextAtSize(text,size),pos,size,f);};
    function newPage(){page=doc.addPage([595.28,841.89]);write('Cielo',left,790,28,serif);rightText(`Pedido #${order.id}`,794,11,bold);page.drawLine({start:{x:left,y:772},end:{x:right,y:772},thickness:.6,color:line});y=742;}
    function ensure(height){if(y-height<55)newPage();}
    function wrap(value,max,size=11,f=font){
      const lines=[];
      for(const paragraph of clean(value).split('\n')){
        let current='';
        for(const word of paragraph.split(/\s+/)){
          if(!word)continue;
          if(f.widthOfTextAtSize(word,size)>max){
            if(current){lines.push(current);current='';}
            for(const char of word){if(f.widthOfTextAtSize(current+char,size)>max){lines.push(current);current='';}current+=char;}
          }else if(current&&f.widthOfTextAtSize(current+' '+word,size)>max){lines.push(current);current=word;}else current+=(current?' ':'')+word;
        }
        lines.push(current);
      }
      return lines;
    }
    function paragraph(value,{size=11,f=font,color=ink,gap=8,max=width}={}){for(const text of wrap(value,max,size,f)){ensure(size*1.45);write(text,left,y,size,f,color);y-=size*1.45;}y-=gap;}
    newPage();paragraph('Información de tu compra',{size:25,f:serif,gap:12});
    paragraph(new Intl.DateTimeFormat('es-PE',{dateStyle:'long',timeStyle:'short',timeZone:'America/Lima'}).format(new Date(order.createdAt)),{size:10,color:muted,gap:20});
    paragraph('Cliente',{size:10,f:bold,color:accent,gap:5});
    paragraph(order.customer||[order.firstName,order.lastName].filter(Boolean).join(' '),{f:bold,gap:4});paragraph(order.email,{gap:4});paragraph(order.phone,{gap:16});
    paragraph('Entrega',{size:10,f:bold,color:accent,gap:5});paragraph([order.address,order.district,order.province,order.department].filter(Boolean).join(', '),{gap:16});
    const states={pendiente:'Pendiente de pago',pagado:'Pago confirmado',preparando:'En preparación',enviado:'En camino',entregado:'Entregado',cancelado:'Cancelado'};
    const payments={yape:'Yape',plin:'Plin',transferencia:'Transferencia bancaria',tarjeta:'Tarjeta'};
    paragraph(`Estado: ${states[order.status]||order.status}`,{gap:4});paragraph(`Medio de pago: ${payments[order.paymentMethod]||order.paymentMethod} · ${order.paymentStatus}`,{gap:20});
    function tableHeader(){ensure(40);write('Producto',left,y,10,bold,muted);write('Cant.',400,y,10,bold,muted);rightText('Importe',y,10,bold);y-=12;page.drawLine({start:{x:left,y},end:{x:right,y},thickness:.6,color:line});y-=22;}
    tableHeader();
    for(const item of order.items||[]){
      const name=wrap(item.name,330,11,bold),extra=wrap([item.engraving?`Grabado: ${item.engraving}`:'',item.size?`Talla: ${item.size}`:''].filter(Boolean).join(' · '),330,10);
      const height=name.length*15+(extra[0]?extra.length*14:0)+24;
      if(y-height<55){newPage();tableHeader();}
      const top=y;for(const text of name){write(text,left,y,11,bold);y-=15;}if(extra[0])for(const text of extra){write(text,left,y,10,font,muted);y-=14;}
      write(item.quantity,407,top,11);rightText(Cielo.money(Number(item.price)*Number(item.quantity)),top,11);y-=14;
      page.drawLine({start:{x:left,y},end:{x:right,y},thickness:.4,color:line});y-=20;
    }
    ensure(120);
    for(const [label,value,f]of [['Subtotal',order.subtotal,font],['Descuento',order.discount,font],['Total de productos',order.total,bold]]){write(label,325,y,11,f);rightText(Cielo.money(value),y,11,f);y-=23;}
    y-=8;paragraph(order.shipping==null?'Envío por cotizar. La tienda coordinará la tarifa y la entrega contigo.':`Envío: ${Cielo.money(order.shipping)}`,{size:10,color:muted,gap:10});
    if(order.giftMessage)paragraph('Mensaje de regalo: '+order.giftMessage,{size:10,color:muted});
    if(order.certificateAvailable&&order.certificateUrl){
      paragraph('Tu certificado de autenticidad y garantía se incluye en la siguiente página.',{size:10,color:accent});
      const response=await fetch(order.certificateUrl,{credentials:'same-origin',cache:'no-store'});
      if(!response.ok)throw Error('No se pudo cargar el certificado. Actualiza el estado del pedido y vuelve a intentarlo.');
      const certificate=await PDFDocument.load(await response.arrayBuffer());const copies=await doc.copyPages(certificate,certificate.getPageIndices());copies.forEach(p=>doc.addPage(p));
    }else paragraph('Tu certificado estará disponible cuando confirmemos el pago del pedido.',{size:10,color:accent});
    return doc.save();
  }
  async function downloadOrder(order,button,errorElement){
    if(!order)return;
    const original=button.innerHTML;
    if(errorElement)errorElement.textContent='';
    await Cielo.busy(button,async()=>{button.textContent='Preparando PDF…';try{const current=await Cielo.api('/orders/'+order.id);const bytes=await buildOrder(current),url=URL.createObjectURL(new Blob([bytes],{type:'application/pdf'}));const link=document.createElement('a');link.href=url;link.download=`Cielo-pedido-${order.id}.pdf`;document.body.append(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),60000);}catch(error){if(errorElement)errorElement.textContent=error.message;else throw error;}finally{button.innerHTML=original;}});
  }
  return{buildOrder,downloadOrder};
})();
