'use strict';
(() => {
 const {api,busy}=Cielo, $=s=>document.querySelector(s),dialog=$('#admin-review-dialog'),form=$('#admin-review-form');
 let current,previewUrl,saving=false;
 const dirty=()=>current && ($('#admin-review-text').value!==current.body || $('#admin-review-photo').files.length>0 || $('#admin-review-photo-url').value.trim()!=='');
 function resetPhoto(){
  if(previewUrl){URL.revokeObjectURL(previewUrl);previewUrl=null;}
  $('#admin-review-photo-url').value='';$('#admin-review-photo').value='';$('#admin-review-photo').setCustomValidity('');
  $('#admin-review-preview').src=current.photo;$('#admin-review-preview').alt=`Fotografía de la reseña de ${current.name}`;
  $('#undo-admin-review-photo').hidden=true;
 }
 async function close(){if(saving)return;if(dirty()&&!(await CieloControls.confirm('¿Descartar los cambios sin guardar?')))return;dialog.close();}
 $('#review-list').addEventListener('click',e=>{
  const button=e.target.closest('[data-edit-review]');if(!button)return;
  current=window.CieloAdminReviews.find(button.dataset.editReview);if(!current)return;
  form.reset();$('#admin-review-text').value=current.body;$('#admin-review-description').textContent=`${current.name} · ${current.product}`;
  $('#admin-review-error').textContent='';resetPhoto();dialog.showModal();$('#admin-review-text').focus({preventScroll:true});
 });
 $('#close-admin-review').addEventListener('click',close);$('#cancel-admin-review').addEventListener('click',close);
 dialog.addEventListener('cancel',e=>{e.preventDefault();close();});
 dialog.addEventListener('close',()=>{if(previewUrl){URL.revokeObjectURL(previewUrl);previewUrl=null;}});
 dialog.addEventListener('keydown',e=>{
  if(e.key!=='Tab')return;
  const controls=[...dialog.querySelectorAll('button:not([disabled]),input:not([disabled]),textarea:not([disabled])')].filter(el=>el.getClientRects().length),first=controls[0],last=controls.at(-1);
  if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus();}
  else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus();}
 });
 $('#undo-admin-review-photo').addEventListener('click',resetPhoto);
 $('#admin-review-photo').addEventListener('change',()=>{
  const field=$('#admin-review-photo'),photo=field.files[0];field.setCustomValidity('');$('#admin-review-error').textContent='';
  if(!photo){resetPhoto();return;}
  if(!['image/jpeg','image/png'].includes(photo.type)||photo.size===0||photo.size>5*1024*1024){field.setCustomValidity('Selecciona una foto JPG o PNG de hasta 5 MB.');CieloControls.validateField(field);return;}
  $('#admin-review-photo-url').value='';if(previewUrl)URL.revokeObjectURL(previewUrl);previewUrl=URL.createObjectURL(photo);
  $('#admin-review-preview').src=previewUrl;$('#admin-review-preview').alt='Vista previa de la nueva fotografía';$('#undo-admin-review-photo').hidden=false;
 });
 $('#admin-review-photo-url').addEventListener('input',()=>{const field=$('#admin-review-photo-url');$('#admin-review-photo').value='';$('#admin-review-photo').setCustomValidity('');if(field.checkValidity())$('#admin-review-preview').src=field.value.trim()||current.photo;$('#undo-admin-review-photo').hidden=!field.value;});
 form.addEventListener('submit',async e=>{
  e.preventDefault();if(saving||!CieloControls.validate(form))return;
  await busy($('#save-admin-review'),async()=>{
   saving=true;$('#admin-review-error').textContent='';$('#close-admin-review').disabled=true;$('#cancel-admin-review').disabled=true;
   try{
    const body=new FormData();body.set('body',$('#admin-review-text').value.trim());const photo=$('#admin-review-photo').files[0];if(photo)body.set('photo',photo);const photoUrl=$('#admin-review-photo-url').value.trim();if(photoUrl)body.set('photoUrl',photoUrl);
    const result=await api('/admin/reviews/'+current.id,{method:'PUT',body});
    await window.CieloAdminReviews.reload();dialog.close();window.CieloAdminReviews.message(result.message);
    document.querySelector(`[data-edit-review="${current.id}"]`)?.focus({preventScroll:true});
   }catch(error){$('#admin-review-error').textContent=error.message;if(!dialog.open)dialog.showModal();$('#admin-review-error').focus();}
   finally{saving=false;$('#close-admin-review').disabled=false;$('#cancel-admin-review').disabled=false;}
  });
 });
})();
