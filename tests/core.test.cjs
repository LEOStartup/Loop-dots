const {test}=require('node:test');const assert=require('node:assert/strict');const C=require('../app/src/main/assets/core.js');
const habit=(entries,extra={})=>({id:'h_test',name:'Teste',color:'#ef4444',target:1,entries,...extra});
test('calendar dates cross February leap day locally',()=>{assert.equal(C.key(C.add(C.date('2024-02-28'),1)),'2024-02-29');assert.equal(C.key(C.add(C.date('2025-02-28'),1)),'2025-03-01')});
test('streak tolerates uncompleted today but not missed yesterday',()=>{let h=habit({'2026-09-21':1,'2026-09-22':1,'2026-09-23':1});assert.deepEqual(C.streak(h,C.date('2026-09-24')),{current:3,best:3,total:3});assert.equal(C.streak(h,C.date('2026-09-25')).current,0)});
test('partial quantities do not count as completed',()=>{let h=habit({'2026-09-21':1,'2026-09-22':2,'2026-09-23':3},{target:3});assert.equal(C.streak(h,C.date('2026-09-23')).total,1)});
test('planned weekdays skip weekend for streak and rate',()=>{let h=habit({'2026-09-18':1,'2026-09-21':1},{schedule:[1,2,3,4,5],created:'2026-09-18'});assert.equal(C.streak(h,C.date('2026-09-21')).current,2);assert.equal(C.rate(h,C.date('2026-09-21')),100)});
test('weekly streak bridges year boundary',()=>{let h=habit({'2025-12-28':1,'2025-12-30':1,'2026-01-06':1});assert.equal(C.periodStreak(h,'week',C.date('2026-01-07')).current,3)});
test('import rejects duplicates and invalid values',()=>{assert.throws(()=>C.validate({habits:[habit({}),habit({})]}));assert.throws(()=>C.validate({habits:[habit({'2026-09-01':-1})]}))});
