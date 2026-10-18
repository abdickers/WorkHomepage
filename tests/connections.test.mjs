import { test } from 'node:test';
import assert from 'node:assert/strict';
import { cleanConnectionPreferences, connectionAccess, connectionReadOutcome } from '../lib/connection-registry.mts';

test('connection preferences accept only known sources and boolean values', () => {
  assert.deepEqual(cleanConnectionPreferences({mail:false,celigo:true,cloudflare:'false',arbitrary:true}), {mail:false,celigo:true});
  for (const value of [null,[],false,'mail']) assert.deepEqual(cleanConnectionPreferences(value),{});
});
test('cached connector metadata never becomes proof of authentication', () => {
  const ctx = (entry) => ({status:'success',connectors:[{connectorId:'test',...entry}]});
  assert.equal(connectionAccess(ctx({policy:'enabled',tools:[{actionName:'read'}]}),'test'),'available');
  assert.equal(connectionAccess(ctx({policy:'enabled',tools:null}),'test'),'unknown');
  assert.equal(connectionAccess(ctx({policy:'enabled',tools:[]}),'test'),'unknown');
  assert.equal(connectionAccess(ctx({policy:'disabled',tools:[{}]}),'test'),'disabled');
  assert.equal(connectionAccess({status:'binding_unavailable'},'test'),'unknown');
});
test('connection checks preserve empty success, failures and partial reads distinctly', () => {
  assert.equal(connectionReadOutcome({status:'success',feeds:{inbox:{status:'success',items:[]}}}).state,'confirmed');
  assert.equal(connectionReadOutcome({status:'success',result:{content:[],structuredContent:{events:[]}}}).state,'confirmed');
  assert.equal(connectionReadOutcome({status:'success',sources:{flows:{status:'success',result:{}},jobs:{status:'upstream_error'}}}).state,'partial');
  for (const value of [null,{status:'success'},{status:'success',sources:{}},{status:'success',feeds:{inbox:{status:'error'}}},{status:'binding_unavailable'}]) assert.equal(connectionReadOutcome(value).state,'unavailable');
});
test('connection checks honor nested retry timing and only server sign-in return paths', () => {
  const data={status:'success',feeds:{inbox:{status:'reauthentication_required',retryAfterMs:120000}},chatGPTSignInPath:'/signin-with-chatgpt?return_to=%2F'};
  const result=connectionReadOutcome(data);
  assert.equal(result.state,'auth');assert.equal(result.retryAfterMs,120000);assert.equal(result.authPath,data.chatGPTSignInPath);
  assert.equal(connectionReadOutcome({...data,chatGPTSignInPath:'https://untrusted.example/'}).authPath,undefined);
});
