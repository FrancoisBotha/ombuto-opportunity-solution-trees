import { type Router, createMemoryHistory, createRouter } from 'vue-router';

import { shallowMount } from '@vue/test-utils';
import axios from 'axios';
import { Subject, type Subscription } from 'rxjs';
import sinon from 'sinon';

import TrackerService from './tracker.service';
import JhiTracker from './tracker.vue';

type JhiTrackerComponentType = InstanceType<typeof JhiTracker>;

let router: Router;

const axiosStub = {
  get: sinon.stub(axios, 'get'),
};

describe('JhiTracker', () => {
  let wrapper: any;
  let trackerView: JhiTrackerComponentType;

  let subject: Subject<any>;
  let subscription: Subscription;
  let subscriptionSpy;
  let trackerServiceStub: any;

  beforeEach(() => {
    axiosStub.get.resolves({ data: {} });
    subject = new Subject<any>();
    trackerServiceStub = sinon.createStubInstance<TrackerService>(TrackerService);
    trackerServiceStub.subscribe = sinon.stub().callsFake(observer => {
      subscription = subject.subscribe(observer);
      subscriptionSpy = sinon.spy(subscription, 'unsubscribe');
      return subscription;
    });

    router = createRouter({ history: createMemoryHistory(), routes: [] });

    wrapper = shallowMount(JhiTracker, {
      global: {
        plugins: [router],
        stubs: {
          'font-awesome-icon': true,
        },
        provide: {
          trackerService: trackerServiceStub,
        },
      },
    });
    trackerView = wrapper.vm;
  });

  it('should subscribe', () => {
    expect(trackerServiceStub.subscribe.calledOnce).toBeTruthy();
  });

  it('should unsubscribe at unmount', () => {
    // WHEN
    wrapper.unmount();

    // THEN
    expect(subscriptionSpy.calledOnce).toBeTruthy();
  });

  it('should add new activity', () => {
    // GIVEN
    const activity1 = { time: '2020-01-01', page: 'login', sessionId: '123' };
    trackerView.activities = [activity1];

    // WHEN
    const activity2 = { time: '2020-01-01', page: 'login', sessionId: '456' };
    subject.next(activity2);

    // THEN
    expect(trackerView.activities).toEqual([activity1, activity2]);
  });

  it('should not add logout activity', () => {
    // WHEN
    subject.next({ time: '2020-01-01', page: 'logout', sessionId: '123' });

    // THEN
    expect(trackerView.activities).toEqual([]);
  });

  it('should update user activity', () => {
    // GIVEN
    trackerView.activities = [{ time: '2020-01-01', page: 'login', sessionId: '123' }];

    // WHEN
    const activity = { time: '2020-01-01', page: 'login', sessionId: '123' };
    subject.next(activity);

    // THEN
    expect(trackerView.activities).toEqual([activity]);
  });

  it('should remove user activity', () => {
    // GIVEN
    trackerView.activities = [{ time: '2020-01-01', page: 'login', sessionId: '123' }];

    // WHEN
    subject.next({ time: '2020-01-01', page: 'logout', sessionId: '123' });

    // THEN
    expect(trackerView.activities).toEqual([]);
  });
});
