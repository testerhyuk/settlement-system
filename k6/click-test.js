import http from 'k6/http';
import { check } from 'k6';

const campaignIds = [
    'campaign-5aa646ab-8c8f-4db4-aa85-7a7a5885fcc7',
//    'campaign-81f918ae-ee11-4729-8f1e-ea40371ecdb2',
//    'campaign-8d5dbe36-5e4c-4487-8036-f0997b5a062e',
//    'campaign-8b3078a7-39d9-4886-8b14-5adfe1304804',
//    'campaign-b9653fe0-2da1-4cf2-a6bc-643294d2e8e6',
//    'campaign-fd71e273-fc63-4967-b967-1ccb8876d78f',
];

export const options = {
    scenarios: {
        constant_request_rate: {
            executor: 'constant-arrival-rate',
            rate: 50,
            timeUnit: '1s',
            duration: '1m',
            preAllocatedVUs: 20,
            maxVUs: 200,
        },
    },
};

export default function () {
    const campaignId = campaignIds[Math.floor(Math.random() * campaignIds.length)];
    const res = http.post(`http://nginx/api/v1/advertisement/ad-campaign/click/${campaignId}`);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}