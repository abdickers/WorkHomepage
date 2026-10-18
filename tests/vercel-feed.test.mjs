import test from 'node:test';
import assert from 'node:assert/strict';
import {projectVercelResult,VERCEL_TEAM_URL,VERCEL_READ_LIMIT} from '../lib/vercel-feed.mts';
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
