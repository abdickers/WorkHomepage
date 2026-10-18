import test from 'node:test';
import assert from 'node:assert/strict';
import {projectVercelResult,projectVercelProfile,VERCEL_TEAM_URL,VERCEL_READ_LIMIT} from '../lib/vercel-feed.mts';
import {readProjectCenter} from '../lib/vercel-project-center.mts';
import {records,providerData} from '../lib/provider-data.mts';
const success = (data) => ({status:'success',result:{content:[{type:'text',text:JSON.stringify({result:data})}],structuredContent:null}});

test('Vercel project projection bounds reads and excludes environment values and metadata',()=>{
 const outcome=projectVercelResult(success({projects:Array.from({length:25},(_,i)=>({id:`prj_${i}`,name:`project-${i}`,env:[{value:'private-value'}],link:{repo:'private-repo'}})),pagination:{next:123}}),'projects');
 assert.equal(outcome.status,'success');assert.equal(records(outcome.result).length,VERCEL_READ_LIMIT);
 assert.deepEqual(records(outcome.result)[0],{id:'prj_0',name:'project-0',href:`${VERCEL_TEAM_URL}/project-0`});
 assert.equal(providerData(outcome.result).hasMore,true);
 assert.ok(!JSON.stringify(outcome).includes('private-value'));
 assert.ok(!JSON.stringify(outcome).includes('private-repo'));
});

test('nested Vercel deployment envelope is read without fabricating an empty feed',()=>{
 const r=projectVercelResult(success({deployments:{pagination:{next:123},deployments:[{id:'dpl_nested',name:'s76h_catalyst',state:'READY',created:1792167340436}]}}),'deployments');
 assert.equal(r.status,'success'); assert.equal(records(r.result)[0].id,'dpl_nested'); assert.equal(providerData(r.result).hasMore,true);
 assert.notEqual(projectVercelResult(success({deployments:{error:'failed'}}),'deployments').status,'success');
});

test('project dossier allows only display metadata and validated domains',()=>{
 const r=projectVercelProfile(success({id:'prj_c',name:'s76h_catalyst',framework:'nextjs',nodeVersion:'24.x',updatedAt:1792167340436,
 domains:['catalyst.example.com',{name:'catalyst.vercel.app'},'evil.example/path?token=secret','javascript:alert(1)','user:password@example.com'],env:[{value:'private-secret'}],link:{repo:'private-repo'}}));
 assert.equal(r.status,'success'); const d=providerData(r.result); assert.deepEqual(d.domains,['catalyst.example.com','catalyst.vercel.app']);
 assert.equal(d.framework,'nextjs'); assert.ok(!JSON.stringify(r).includes('private-secret')); assert.ok(!JSON.stringify(r).includes('private-repo'));
 assert.notEqual(projectVercelProfile(success({name:'missing-id'})).status,'success');
});

test('Catalyst watch ignores unrelated projects and fetches production independently of busy previews',async()=>{
 const calls=[];
 const client={invoke:async(connector,action,args)=>{
  calls.push({connector,action,args});
  if(action==='get_project')return success({id:'prj_c',name:'s76h_catalyst',domains:[]});
  return success({deployments:{deployments:Array.from({length:12},(_,i)=>({id:`dpl_${i}`,name:'s76h_catalyst',state:'READY',target:args.target||null,created:1792167340436-i}))}});
 }};
 const result=await readProjectCenter(client,projectVercelResult(success({projects:[{id:'prj_c',name:'s76h_catalyst'},{id:'other',name:'unrelated-project'}]}),'projects'));
 assert.equal(result.length,1); assert.equal(result[0].key,'s76_catalyst');assert.equal(providerData(result[0].profile.result).name,'s76h_catalyst');
 assert.equal(records(result[0].deployments.result).length,8); assert.equal(records(result[0].production.result).length,1);
 assert.equal(calls.length,3);assert.ok(calls.every(c=>c.args.slug==='spirit-of-76-fireworks'&&(c.args.projectId||c.args.idOrName)==='prj_c'));
 assert.ok(calls.some(c=>c.args.target==='production'&&c.args.limit===1));
 const unavailable=await readProjectCenter(client,{status:'reauthentication_required',message:'Sign in'});
 assert.ok(unavailable.every(w=>w.profile.status==='reauthentication_required'));assert.equal(calls.length,3);
 const missing=await readProjectCenter(client,projectVercelResult(success({projects:[{id:'other',name:'bc-proline'}]}),'projects'));
 assert.equal(missing.length,1);assert.notEqual(missing[0].profile.status,'success');assert.match(missing[0].profile.message,/not visible/);assert.equal(calls.length,3);
});
test('Vercel denied, malformed and MCP error results cannot become healthy empty feeds',()=>{
 for(const input of [{status:'tool_error',message:'Forbidden',retryAfterMs:120000},success({error:{code:'forbidden'},projects:[]}),success({other:[]}),{status:'success',result:{isError:true,structuredContent:{projects:[]}}},success({projects:[{name:'no-id'}]})]){
  const outcome=projectVercelResult(input,'projects');assert.notEqual(outcome.status,'success');
 }
 assert.equal(projectVercelResult({status:'reauthentication_required',message:'Sign in',retryAfterMs:120000},'projects').status,'reauthentication_required');
 assert.equal(projectVercelResult({status:'rate_limited',message:'Slow down',retryAfterMs:120000},'projects').retryAfterMs,120000);
 assert.deepEqual(records(projectVercelResult(success({projects:[],pagination:{next:null}}),'projects').result),[]);
});
test('deployment states, null preview targets and links remain scoped to the requested team',()=>{
 const result=projectVercelResult(success({deployments:[
  {uid:'dpl_1',name:'store',state:'READY',target:'production',createdAt:1792353800000,inspectorUrl:`${VERCEL_TEAM_URL}/store/observed-id?token=private`},
  {uid:'dpl_2',name:'store',readyState:'BUILDING',target:null,created:1792353800000,inspectorUrl:'https://vercel.com/other-team/store/id'},
  {uid:'dpl_3',name:'../outside',state:'future-status',createdAt:'invalid',inspectorUrl:'https://attacker.example/'}
 ]}),'deployments');
 const rows=records(result.result);
 assert.equal(rows[0].state,'READY');assert.equal(rows[0].target,'production');assert.equal(rows[0].href,`${VERCEL_TEAM_URL}/store/observed-id`);
 assert.equal(rows[1].state,'BUILDING');assert.equal(rows[1].target,'preview');assert.equal(rows[1].href,`${VERCEL_TEAM_URL}/store`);
 assert.equal(rows[2].state,'UNKNOWN');assert.equal(rows[2].createdAt,null);assert.equal(rows[2].href,VERCEL_TEAM_URL);
});
