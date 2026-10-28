import test from 'node:test';
import assert from 'node:assert/strict';
import { slackSearchAction, SLACK_CONNECTOR } from '../lib/slack-search.mts';
import type { ConnectorContext } from '../lib/connector-contract.mts';
const schema = {type:'object',properties:{keywords:{type:'array'},filters:{type:'string'},limit:{type:'number'}}};
function context(actionName: string): ConnectorContext {return {status:'success',connectors:[{connectorId:SLACK_CONNECTOR,policy:'enabled',tools:[{actionName,inputSchema:schema}]}]};}
test('uses the canonical private-search action when it is exposed',()=>assert.equal(slackSearchAction(context('slack_search_public_and_private')),'slack_search_public_and_private'));
test('uses an exact qualified name from runtime metadata rather than fabricating it',()=>assert.equal(slackSearchAction(context('slack.slack_search_public_and_private')),'slack.slack_search_public_and_private'));
test('does not substitute public-only search for a private-message feed',()=>assert.equal(slackSearchAction(context('slack_search_public')),'slack_search_public_and_private'));
test('null cached tools do not prevent the known live read',()=>assert.equal(slackSearchAction({status:'success',connectors:[{connectorId:SLACK_CONNECTOR,policy:'enabled',tools:null}]}),'slack_search_public_and_private'));
