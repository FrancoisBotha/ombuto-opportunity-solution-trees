import { beforeEach, describe, expect, it } from 'vitest';

import axios from 'axios';
import dayjs from 'dayjs';
import sinon from 'sinon';

import { DATE_FORMAT, DATE_TIME_FORMAT } from '@/shared/composables/date-format';
import { Outcome } from '@/shared/model/outcome.model';

import OutcomeService from './outcome.service';

const error = {
  response: {
    status: null,
    data: {
      type: null,
    },
  },
};

const axiosStub = {
  get: sinon.stub(axios, 'get'),
  post: sinon.stub(axios, 'post'),
  put: sinon.stub(axios, 'put'),
  patch: sinon.stub(axios, 'patch'),
  delete: sinon.stub(axios, 'delete'),
};

describe('Service Tests', () => {
  describe('Outcome Service', () => {
    let service: OutcomeService;
    let elemDefault;
    let currentDate: Date;

    beforeEach(() => {
      service = new OutcomeService();
      currentDate = new Date();
      elemDefault = new Outcome(
        123,
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'DRAFT',
        currentDate,
        currentDate,
        0,
        currentDate,
        currentDate,
      );
    });

    describe('Service methods', () => {
      it('should find an element', async () => {
        const returnedFromService = {
          startDate: dayjs(currentDate).format(DATE_FORMAT),
          targetDate: dayjs(currentDate).format(DATE_FORMAT),
          createdDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          lastModifiedDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...elemDefault,
        };
        axiosStub.get.resolves({ data: returnedFromService });

        return service.find(123).then(res => {
          expect(res).toMatchObject(elemDefault);
        });
      });

      it('should not find an element', async () => {
        axiosStub.get.rejects(error);
        return service
          .find(123)
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should create a Outcome', async () => {
        const returnedFromService = {
          id: 123,
          startDate: dayjs(currentDate).format(DATE_FORMAT),
          targetDate: dayjs(currentDate).format(DATE_FORMAT),
          createdDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          lastModifiedDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...elemDefault,
        };
        const expected = {
          startDate: currentDate,
          targetDate: currentDate,
          createdDate: currentDate,
          lastModifiedDate: currentDate,
          ...returnedFromService,
        };

        axiosStub.post.resolves({ data: returnedFromService });
        return service.create({}).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not create a Outcome', async () => {
        axiosStub.post.rejects(error);

        return service
          .create({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should update a Outcome', async () => {
        const returnedFromService = {
          title: 'BBBBBB',
          description: 'BBBBBB',
          metric: 'BBBBBB',
          targetValue: 'BBBBBB',
          currentValue: 'BBBBBB',
          status: 'BBBBBB',
          startDate: dayjs(currentDate).format(DATE_FORMAT),
          targetDate: dayjs(currentDate).format(DATE_FORMAT),
          sortOrder: 1,
          createdDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          lastModifiedDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...elemDefault,
        };

        const expected = {
          startDate: currentDate,
          targetDate: currentDate,
          createdDate: currentDate,
          lastModifiedDate: currentDate,
          ...returnedFromService,
        };
        axiosStub.put.resolves({ data: returnedFromService });

        return service.update(expected).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not update a Outcome', async () => {
        axiosStub.put.rejects(error);

        return service
          .update({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should partial update a Outcome', async () => {
        const patchObject = {
          title: 'BBBBBB',
          description: 'BBBBBB',
          targetValue: 'BBBBBB',
          startDate: dayjs(currentDate).format(DATE_FORMAT),
          sortOrder: 1,
          createdDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          lastModifiedDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...new Outcome(),
        };
        const returnedFromService = Object.assign(patchObject, elemDefault);

        const expected = {
          startDate: currentDate,
          targetDate: currentDate,
          createdDate: currentDate,
          lastModifiedDate: currentDate,
          ...returnedFromService,
        };
        axiosStub.patch.resolves({ data: returnedFromService });

        return service.partialUpdate(patchObject).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not partial update a Outcome', async () => {
        axiosStub.patch.rejects(error);

        return service
          .partialUpdate({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should return a list of Outcome', async () => {
        const returnedFromService = {
          title: 'BBBBBB',
          description: 'BBBBBB',
          metric: 'BBBBBB',
          targetValue: 'BBBBBB',
          currentValue: 'BBBBBB',
          status: 'BBBBBB',
          startDate: dayjs(currentDate).format(DATE_FORMAT),
          targetDate: dayjs(currentDate).format(DATE_FORMAT),
          sortOrder: 1,
          createdDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          lastModifiedDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...elemDefault,
        };
        const expected = {
          startDate: currentDate,
          targetDate: currentDate,
          createdDate: currentDate,
          lastModifiedDate: currentDate,
          ...returnedFromService,
        };
        axiosStub.get.resolves([returnedFromService]);
        return service.retrieve().then(res => {
          expect(res).toContainEqual(expected);
        });
      });

      it('should not return a list of Outcome', async () => {
        axiosStub.get.rejects(error);

        return service
          .retrieve()
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should delete a Outcome', async () => {
        axiosStub.delete.resolves({ ok: true });
        return service.delete(123).then(res => {
          expect(res.ok).toBeTruthy();
        });
      });

      it('should not delete a Outcome', async () => {
        axiosStub.delete.rejects(error);

        return service
          .delete(123)
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });
    });
  });
});
