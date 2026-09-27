const assert = require('node:assert/strict');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({headless:true});
  try {
    const page = await browser.newPage({viewport:{width:393,height:852}});
    const errors=[]; page.on('pageerror',error=>errors.push(error.message));
    await page.goto(pathToFileURL(path.resolve(__dirname,'../app/src/main/assets/index.html')).href);
    const result=await page.evaluate(()=>{
      const habit=(id,archived=false)=>({id,name:id,icon:'✓',color:'#ef4444',entries:{'2026-09-20':1},notes:{'2026-09-20':'nota'},created:'2026-09-20',target:1,archived,schedule:[],reminderDays:[],reminderTime:'12:00'});
      state.habits=[habit('remove'),habit('keep'),habit('old',true)];selected='remove';persist();render();
      openMenu('remove',180,300);
      const menuHasDelete=!!document.querySelector('.menu button.danger');
      document.querySelector('.menu button.danger').click();
      const confirmKeepsData=!!get('remove')&&!!document.querySelector('#confirmDelete');
      document.querySelector('#cancelDelete').click();closeSheet(true);
      const cancelKeepsData=!!get('remove')&&get('remove').entries['2026-09-20']===1;
      openMenu('remove',180,300);document.querySelector('.menu button.danger').click();
      document.querySelector('#confirmDelete').click();
      const activeDeleted=!get('remove')&&!!get('keep')&&selected==='keep'&&
        !JSON.parse(localStorage.getItem('loopdots')).habits.some(h=>h.id==='remove');
      setting('archived');document.querySelector('.secondary.danger').click();
      document.querySelector('#confirmDelete').click();
      return {menuHasDelete,confirmKeepsData,cancelKeepsData,activeDeleted,archivedDeleted:!get('old')&&!!get('keep')};
    });
    assert.deepEqual(result,{menuHasDelete:true,confirmKeepsData:true,cancelKeepsData:true,activeDeleted:true,archivedDeleted:true});
    assert.deepEqual(errors,[]);
    console.log('PASS: active and archived habit deletion requires confirmation and preserves other habits');
  } finally {await browser.close()}
})().catch(error=>{console.error(error);process.exitCode=1});
