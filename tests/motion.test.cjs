const assert = require('node:assert/strict');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { chromium } = require('playwright');

(async () => {
  const options = { headless:true };
  if (process.env.CHROMIUM_EXECUTABLE) options.executablePath = process.env.CHROMIUM_EXECUTABLE;
  if (process.env.CHROMIUM_ARGS) options.args = JSON.parse(process.env.CHROMIUM_ARGS);
  const browser = await chromium.launch(options);
  try {
    const page = await browser.newPage({viewport:{width:393,height:852},hasTouch:true});
    const errors = []; page.on('pageerror', e=>errors.push(e.message));
    await page.goto(pathToFileURL(path.resolve(__dirname,'../app/src/main/assets/index.html')).href);
    await page.evaluate(()=>{
      state.habits=[{id:'test',name:'Meditar',description:'',color:'#a855f7',icon:'🧘',entries:{},notes:{},target:1,created:todayKey(),categories:[],reminderDays:[],schedule:[],reminderTime:'12:00',showStreak:true}];
      persist();render();window.originalCard=document.querySelector('[data-card]');window.originalNav=document.querySelector('#nav button');
    });
    // Repeated marks update the live day/card. No card entrance or page animation.
    for(let i=0;i<5;i++) await page.evaluate(()=>mark('test'));
    assert.deepEqual(await page.evaluate(()=>({card:originalCard===document.querySelector('[data-card]'),nav:originalNav===document.querySelector('#nav button'),count:get('test').entries[todayKey()],cardAnimations:originalCard.getAnimations().length})),{card:true,nav:true,count:1,cardAnimations:0});

    await page.evaluate(()=>calendar('test')); await page.waitForTimeout(380);
    const stable = await page.evaluate(async()=>{
      const panel=document.querySelector('.sheet'),scrim=document.querySelector('.scrim'),day=panel.querySelector('[data-date="'+todayKey()+'"]');
      const top=panel.getBoundingClientRect().top;day.click();const positions=[];
      for(let i=0;i<20;i++){await new Promise(requestAnimationFrame);positions.push(panel.getBoundingClientRect().top)}
      return {samePanel:panel===document.querySelector('.sheet'),sameScrim:scrim===document.querySelector('.scrim'),sameDay:day===panel.querySelector('[data-date="'+todayKey()+'"]'),maxShift:Math.max(...positions.map(y=>Math.abs(y-top))),count:get('test').entries[todayKey()]};
    });
    assert.deepEqual(stable,{samePanel:true,sameScrim:true,sameDay:true,maxShift:0,count:0});
    const calendarTop=await page.locator('.sheet').evaluate(el=>el.getBoundingClientRect().top);
    await page.evaluate(()=>{for(let i=0;i<8;i++)shiftCalendar('test',-1)});await page.waitForTimeout(400);
    assert.equal(await page.locator('.sheet').evaluate(el=>el.getBoundingClientRect().top),calendarTop);
    assert.equal(await page.locator('.calday').count(),42);
    assert.equal(await page.locator('.transition-copy').count(),0);

    // Editing retains keyboard focus, caret, expanded options and scroll position.
    await page.evaluate(()=>{closeSheet(true);editHabit('test')});await page.waitForTimeout(380);
    assert.equal(await page.evaluate(()=>{
      const input=document.querySelector('#name');input.focus();input.value='Meditar com calma';input.setSelectionRange(5,5);captureDraft();draft.color='#ef4444';editForm();
      return input===document.querySelector('#name')&&document.activeElement===input&&input.selectionStart===5;
    }),true);
    assert.deepEqual(await page.evaluate(()=>{
      document.activeElement.blur();const panel=document.querySelector('.sheet');const detail=panel.querySelector('details');detail.open=true;panel.scrollTop=panel.scrollHeight;const before=panel.scrollTop;
      captureDraft();draft.schedule=[1,3,5];editForm();return {open:detail.open,same:panel===document.querySelector('.sheet'),scroll:panel.scrollTop===before};
    }),{open:true,same:true,scroll:true});
    const returnScroll=await page.locator('.sheet').evaluate(el=>el.scrollTop);
    await page.evaluate(()=>{captureDraft();emojiPicker()});await page.waitForTimeout(320);
    const emojiStable=await page.evaluate(()=>{const panel=document.querySelector('.sheet'),scrim=document.querySelector('.scrim');emojiPicker('Saúde');return panel===document.querySelector('.sheet')&&scrim===document.querySelector('.scrim')&&panel.getAnimations().length===0});
    assert.equal(emojiStable,true);
    await page.evaluate(()=>sheetBack());await page.waitForTimeout(340);
    assert.equal(await page.locator('#name').inputValue(),'Meditar com calma');
    assert.equal(await page.locator('details').getAttribute('open'),'');
    assert.equal(await page.locator('.sheet').evaluate(el=>el.scrollTop),returnScroll);

    // Closing is animated; reopening mid-close must not let a stale callback remove it.
    await page.evaluate(()=>{closeSheet();setting('general')});await page.waitForTimeout(400);
    assert.equal(await page.locator('.sheet').count(),1);
    assert.equal(await page.locator('.scrim').evaluate(el=>getComputedStyle(el).opacity),'1');
    await page.evaluate(()=>closeSheet());
    assert.equal(await page.locator('.sheet').count(),1);
    await page.waitForTimeout(320);
    assert.equal(await page.locator('.sheet').count(),0);
    assert.equal(await page.locator('body').evaluate(el=>el.classList.contains('overlay-open')),false);

    await page.evaluate(()=>navigate('settings'));await page.waitForTimeout(320);
    await page.getByRole('button',{name:'Geral',exact:true}).click();await page.waitForTimeout(380);
    assert.equal(await page.locator('.page-panel').count(),1);
    await page.getByRole('button',{name:'Vibração',exact:true}).click();
    assert.equal(await page.locator('.sheet').evaluate(el=>el.getAnimations().length),0);
    assert.equal(await page.locator('.switch').first().evaluate(el=>Math.round(el.getBoundingClientRect().width)),42);
    await page.evaluate(()=>{closeSheet(true);for(const p of ['month','week','year','stats','settings','month'])navigate(p)});await page.waitForTimeout(400);
    assert.equal(await page.locator('.transition-copy').count(),0);
    assert.equal(await page.locator('#nav .active').getAttribute('aria-label'),'Mês');
    // Record the bar DURING transitions, not just after they have settled.
    const navGeometry = await page.evaluate(async()=>{
      const nav=document.querySelector('#nav'),main=document.querySelector('#app');
      const box=()=>{const r=nav.getBoundingClientRect();return [r.x,r.y,r.width,r.height]};
      const initial=box(),frames=[];
      for(const tab of ['settings','stats','week','month']){
        navigate(tab);
        for(let i=0;i<16;i++){await new Promise(requestAnimationFrame);frames.push({box:box(),opacity:getComputedStyle(main).opacity,transform:getComputedStyle(main).transform})}
      }
      navigate('settings');main.scrollTop=main.scrollHeight;const scroll=main.scrollTop;
      setting('general');
      for(let i=0;i<22;i++){await new Promise(requestAnimationFrame);frames.push({box:box(),opacity:getComputedStyle(main).opacity,transform:getComputedStyle(main).transform})}
      closeSheet();
      for(let i=0;i<22;i++){await new Promise(requestAnimationFrame);frames.push({box:box(),opacity:getComputedStyle(main).opacity,transform:getComputedStyle(main).transform})}
      return {stable:frames.every(f=>f.box.every((n,i)=>Math.abs(n-initial[i])<.1)),painted:frames.every(f=>f.opacity==='1'&&f.transform==='none'),scrolled:scroll>0,restored:main.scrollTop===scroll,documentScroll:scrollY};
    });
    assert.deepEqual(navGeometry,{stable:true,painted:true,scrolled:true,restored:true,documentScroll:0});
    const button=page.locator('#nav button').first(),before=await button.boundingBox();
    await button.hover();await page.mouse.down();await page.waitForTimeout(100);
    assert.deepEqual(await button.boundingBox(),before);await page.mouse.up();
    await page.emulateMedia({reducedMotion:'reduce'});
    await page.evaluate(()=>{navigate('week');editHabit('test')});
    assert.equal(await page.evaluate(()=>document.getAnimations().length),0);
    await page.evaluate(()=>closeSheet());assert.equal(await page.locator('.sheet').count(),0);
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
    assert.deepEqual(errors,[]);
    console.log('PASS: retained controls, stable calendar frames, focus/caret/scroll, nested return, close/reopen race, rapid tabs, fixed navigation during transitions/press/scroll, reduced motion');
  } finally {await browser.close()}
})().catch(error=>{console.error(error);process.exitCode=1});
