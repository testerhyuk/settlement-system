import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://nginx';
const TEST_MODE = __ENV.TEST_MODE || 'smoke';

const CAMPAIGN_ID = __ENV.CAMPAIGN_ID || 'campaign-d85b5d29-6036-46a7-9cfd-373a4c0e3c99';
const ADVERTISER_ID = __ENV.ADVERTISER_ID || 'advertiser-28aa3b71-077b-4d45-a80e-21a35c4186dd';
const USER_ID_PREFIX = __ENV.USER_ID_PREFIX || 'load-user';
const FIXED_USER_ID = __ENV.FIXED_USER_ID || '';
const CPC_AMOUNT = Number(__ENV.CPC_AMOUNT || 1);
const CONVERSION_AMOUNT = Number(__ENV.CONVERSION_AMOUNT || 15000);

const scenariosByMode = {
  smoke: {
    mixed_ad_events: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 20),
      timeUnit: '1s',
      duration: __ENV.DURATION || '1m',
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 20),
      maxVUs: Number(__ENV.MAX_VUS || 100),
    },
  },
  load: {
    mixed_ad_events: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 150),
      timeUnit: '1s',
      duration: __ENV.DURATION || '5m',
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 100),
      maxVUs: Number(__ENV.MAX_VUS || 500),
    },
  },
  stress: {
    mixed_ad_events: {
      executor: 'ramping-arrival-rate',
      startRate: Number(__ENV.START_RATE || 100),
      timeUnit: '1s',
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 150),
      maxVUs: Number(__ENV.MAX_VUS || 800),
      stages: [
        { target: Number(__ENV.STRESS_TARGET_1 || 200), duration: __ENV.STRESS_STAGE_1 || '2m' },
        { target: Number(__ENV.STRESS_TARGET_2 || 400), duration: __ENV.STRESS_STAGE_2 || '3m' },
        { target: Number(__ENV.STRESS_TARGET_3 || 600), duration: __ENV.STRESS_STAGE_3 || '3m' },
        { target: 0, duration: __ENV.STRESS_COOLDOWN || '1m' },
      ],
    },
  },
};

export const options = {
  scenarios: scenariosByMode[TEST_MODE] || scenariosByMode.smoke,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

function headers() {
  return {
    headers: {
      'Content-Type': 'application/json',
    },
  };
}

function checkOk(name, response) {
  check(response, {
    [`${name} status is 2xx`]: (r) => r.status >= 200 && r.status < 300,
  });
}

function eventSuffix() {
  return `${exec.scenario.iterationInTest}-${Date.now()}-${Math.floor(Math.random() * 1000000)}`;
}

function userId() {
  if (FIXED_USER_ID) {
    return FIXED_USER_ID;
  }

  return `${USER_ID_PREFIX}-${exec.vu.idInTest % 1000}`;
}

function sendImpression(suffix, currentUserId) {
  const impressionId = `load-impression-${suffix}`;

  const response = http.post(
    `${BASE_URL}/api/ad-serving/impressions`,
    JSON.stringify([
      {
        impressionId,
        campaignId: CAMPAIGN_ID,
        advertiserId: ADVERTISER_ID,
        userId: currentUserId,
      },
    ]),
    headers()
  );

  checkOk('impression', response);
  return impressionId;
}

function sendClick(suffix, impressionId, currentUserId) {
  const clickId = `load-click-${suffix}`;

  const clickResponse = http.post(
    `${BASE_URL}/api/ad-serving/clicks`,
    JSON.stringify([
      {
        clickId,
        impressionId,
        campaignId: CAMPAIGN_ID,
        advertiserId: ADVERTISER_ID,
        userId: currentUserId,
        cpcAmount: CPC_AMOUNT,
        currency: 'KRW',
      },
    ]),
    headers()
  );

  checkOk('click event', clickResponse);

  const budgetResponse = http.post(`${BASE_URL}/api/v1/advertisement/ad-campaign/click/${CAMPAIGN_ID}`);
  checkOk('budget deduct', budgetResponse);

  return clickId;
}

function sendConversion(suffix, impressionId, clickId, currentUserId) {
  const response = http.post(
    `${BASE_URL}/api/ad-serving/conversions`,
    JSON.stringify([
      {
        clickId,
        impressionId,
        campaignId: CAMPAIGN_ID,
        advertiserId: ADVERTISER_ID,
        userId: currentUserId,
        conversionAmount: CONVERSION_AMOUNT,
      },
    ]),
    headers()
  );

  checkOk('conversion', response);
}

export default function () {
  const suffix = eventSuffix();
  const currentUserId = userId();

  const impressionId = sendImpression(suffix, currentUserId);

  const roll = Math.random();
  if (roll < 0.08) {
    const clickId = sendClick(suffix, impressionId, currentUserId);

    if (Math.random() < 0.15) {
      sendConversion(suffix, impressionId, clickId, currentUserId);
    }
  }

  sleep(0.01);
}
