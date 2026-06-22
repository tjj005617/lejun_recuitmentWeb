const https = require('https');

const API_KEY = 'tp-cjn6l45egnx146gm77kvdgm3gyh7h6l1kthigy53oht284io';
const API_URL = 'https://token-plan-cn.xiaomimimo.com/anthropic/v1/messages';
const MODEL = 'mimo-v2.5-pro';

const body = JSON.stringify({
  model: MODEL,
  max_tokens: 100,
  messages: [{ role: 'user', content: '你好，请用一句话介绍自己' }]
});

const url = new URL(API_URL);
const options = {
  hostname: url.hostname,
  path: url.pathname,
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'x-api-key': API_KEY,
    'anthropic-version': '2023-06-01',
    'Content-Length': Buffer.byteLength(body)
  }
};

const req = https.request(options, (res) => {
  let data = '';
  res.on('data', (chunk) => data += chunk);
  res.on('end', () => {
    console.log('Status:', res.statusCode);
    try {
      const json = JSON.parse(data);
      if (json.content) {
        console.log('AI回复:', json.content[0].text);
      } else {
        console.log('响应:', data);
      }
    } catch (e) {
      console.log('响应:', data);
    }
  });
});

req.on('error', (e) => console.error('错误:', e.message));
req.write(body);
req.end();
