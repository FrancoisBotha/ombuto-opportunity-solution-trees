import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import TeamService from '@/entities/team/team.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import ProductUpdate from './product-update.vue';
import ProductService from './product.service';

type ProductUpdateComponentType = InstanceType<typeof ProductUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const productSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<ProductUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('Product Management Update Component', () => {
    let comp: ProductUpdateComponentType;
    let productServiceStub: SinonStubbedInstance<ProductService>;

    beforeEach(() => {
      route = {};
      productServiceStub = sinon.createStubInstance<ProductService>(ProductService);
      productServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

      alertService = new AlertService({
        toast: {
          show: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          'font-awesome-icon': true,
          'b-input-group': true,
          'b-input-group-prepend': true,
          'b-form-datepicker': true,
          'b-form-input': true,
        },
        provide: {
          alertService,
          productService: () => productServiceStub,
          teamService: () =>
            sinon.createStubInstance<TeamService>(TeamService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
        },
      };
    });

    afterEach(() => {
      vitest.resetAllMocks();
    });

    describe('load', () => {
      beforeEach(() => {
        const wrapper = shallowMount(ProductUpdate, { global: mountOptions });
        comp = wrapper.vm;
      });
      it('Should convert date from string', () => {
        // GIVEN
        const date = new Date('2019-10-15T11:42:02Z');

        // WHEN
        const convertedDate = comp.convertDateTimeFromServer(date);

        // THEN
        expect(convertedDate).toEqual(dayjs(date).format(DATE_TIME_LONG_FORMAT));
      });

      it('Should not convert date if date is not present', () => {
        expect(comp.convertDateTimeFromServer(null)).toBeNull();
      });
    });

    describe('sort order (server-owned)', () => {
      it('is read-only with a hint and not required', () => {
        const wrapper = shallowMount(ProductUpdate, { global: mountOptions });
        const field = wrapper.get('[data-cy="sortOrder"]');
        expect(field.attributes('readonly')).toBeDefined();
        expect(field.attributes('required')).toBeUndefined();
        expect(wrapper.get('[data-cy="sortOrderHelp"]').text()).toContain('Set by the server');
        expect((wrapper.vm as any).v$.sortOrder.required).toBeUndefined();
      });
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', async () => {
        // GIVEN
        const wrapper = shallowMount(ProductUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.product = productSample;
        productServiceStub.update.resolves(productSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(productServiceStub.update.calledWith(productSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        productServiceStub.create.resolves(entity);
        const wrapper = shallowMount(ProductUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.product = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(productServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        productServiceStub.find.resolves(productSample);
        productServiceStub.retrieve.resolves([productSample]);

        // WHEN
        route = {
          params: {
            productId: `${productSample.id}`,
          },
        };
        const wrapper = shallowMount(ProductUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.product).toMatchObject(productSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        productServiceStub.find.resolves(productSample);
        const wrapper = shallowMount(ProductUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
