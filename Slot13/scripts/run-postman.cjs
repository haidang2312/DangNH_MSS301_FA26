const fs = require('node:fs');
const {execFileSync} = require('node:child_process');
const newman = require('newman');
const source = JSON.parse(fs.readFileSync('Saga_Postman_Collection.json', 'utf8'));
const urls = {orderUrl: process.env.ORDER_URL || 'http://localhost:8081', inventoryUrl: process.env.INVENTORY_URL || 'http://localhost:8082', paymentUrl: process.env.PAYMENT_URL || 'http://localhost:8083'};
source.variable.forEach(v => {if (urls[v.key]) v.value = urls[v.key];});
fs.mkdirSync('evidence', {recursive:true});
let state = source;
let failed = false;
function run(label, items) {
    const collection = {...state, item:items};
    let variables = collection.variable;
    return new Promise((resolve,reject) => newman.run({collection, reporters:['cli','json'], reporter:{json:{export:`evidence/${label}.json`}}}, (err, summary) => {
        if(err) return reject(err);
        state = {...summary.collection.toJSON(), variable:variables};
        const failures = summary.run.failures;
        failed ||= failures.length > 0;
        const brief = {label, requests:summary.run.stats.requests, assertions:summary.run.stats.assertions, failures:failures.map(f=>({name:f.error.name,message:f.error.message,source:f.source.name}))};
        fs.writeFileSync(`evidence/${label}-summary.json`, JSON.stringify(brief,null,2));
        for(const execution of summary.run.executions) {
            if(execution.item.name === '4. Saga logs' && execution.response) fs.writeFileSync(`evidence/${label}-saga-${execution.cursor.iteration}-${execution.cursor.position}.json`, execution.response.stream);
        }
        resolve(summary);
    }).on('script', (err, args) => {
        const scope = args.execution && args.execution.collectionVariables;
        if (scope) variables = scope.toJSON().values;
    }));
}
function compose(...args) {execFileSync('docker', ['compose', ...args], {stdio:'inherit'});}
async function ready(url) {
    for(let i=0;i<90;i++) {
        try {if((await fetch(url)).ok) return;} catch {}
        await new Promise(r=>setTimeout(r,1000));
    }
    throw new Error(`Service did not become ready: ${url}`);
}
(async()=>{
    await Promise.all([ready(urls.orderUrl+'/api/orders'),ready(urls.inventoryUrl+'/api/inventory/products'),ready(urls.paymentUrl+'/api/payments/wallets')]);
    if (!process.env.ONLY_KB6) await run('runner', source.item.filter(f=>f.name!=='Manual'));
    if(failed) throw new Error('Collection assertions failed');
    const kb6 = source.item.find(f=>f.name==='Manual').item.find(f=>f.name.startsWith('KB6'));
    await run('kb6-snapshot', [kb6.item[0]]);
    compose('stop','payment-service');
    try {await run('kb6-payment-down', [kb6.item[1]]);}
    finally {compose('start','payment-service');await ready(urls.paymentUrl+'/api/payments/wallets');}
    await run('kb6-verification', kb6.item.slice(2));
    fs.writeFileSync('evidence/docker-compose-ps.txt',execFileSync('docker',['compose','ps'],{encoding:'utf8'}));
    process.exitCode=failed?1:0;
})().catch(err=>{console.error(err);process.exitCode=1;});
