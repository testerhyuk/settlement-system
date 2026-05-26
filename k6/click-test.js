import http from 'k6/http';
import { check } from 'k6';

const campaignIds = [
//    'campaign-f5e04fcf-45af-47f8-9dfa-fdc81ea318a7',
    'campaign-f3110fc2-e811-4114-8f8c-c2b21f6664a0',
//    'campaign-6763b0b4-bcdd-4234-a0d1-494da4587ef6',
//    'campaign-34f57846-9cb9-4d19-9efb-4fead05452b7',
//    'campaign-c4e969ab-35ce-4e9a-a340-5644ea7a21ac',
];

export const options = {
    scenarios: {
        constant_request_rate: {
            executor: 'constant-arrival-rate',
            rate: 100,
            timeUnit: '1s',
            duration: '1m',
            preAllocatedVUs: 20,
            maxVUs: 50,
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