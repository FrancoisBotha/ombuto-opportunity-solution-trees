import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import CommentDetails from './comment-details.vue';
import CommentService from './comment.service';

type CommentDetailsComponentType = InstanceType<typeof CommentDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const commentSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Comment Management Detail Component', () => {
    let commentServiceStub: SinonStubbedInstance<CommentService>;
    let mountOptions: MountingOptions<CommentDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      commentServiceStub = sinon.createStubInstance<CommentService>(CommentService);

      alertService = new AlertService({
        toast: {
          show: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          'font-awesome-icon': true,
          'router-link': true,
        },
        provide: {
          alertService,
          commentService: () => commentServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        commentServiceStub.find.resolves(commentSample);
        route = {
          params: {
            commentId: `${123}`,
          },
        };
        const wrapper = shallowMount(CommentDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.comment).toMatchObject(commentSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        commentServiceStub.find.resolves(commentSample);
        const wrapper = shallowMount(CommentDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
