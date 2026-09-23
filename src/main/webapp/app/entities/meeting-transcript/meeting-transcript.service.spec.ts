import { beforeEach, describe, expect, it } from 'vitest';

import axios from 'axios';
import dayjs from 'dayjs';
import sinon from 'sinon';

import { DATE_FORMAT, DATE_TIME_FORMAT } from '@/shared/composables/date-format';
import { MeetingTranscript } from '@/shared/model/meeting-transcript.model';

import MeetingTranscriptService from './meeting-transcript.service';

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
  describe('MeetingTranscript Service', () => {
    let service: MeetingTranscriptService;
    let elemDefault;
    let currentDate: Date;

    beforeEach(() => {
      service = new MeetingTranscriptService();
      currentDate = new Date();
      elemDefault = new MeetingTranscript(123, 'AAAAAAA', currentDate, 'AAAAAAA', 'AAAAAAA', 'PASTED', currentDate, currentDate);
    });

    describe('Service methods', () => {
      it('should find an element', async () => {
        const returnedFromService = {
          meetingDate: dayjs(currentDate).format(DATE_FORMAT),
          createdDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          editedDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
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

      it('should create a MeetingTranscript', async () => {
        const returnedFromService = {
          id: 123,
          meetingDate: dayjs(currentDate).format(DATE_FORMAT),
          createdDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          editedDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...elemDefault,
        };
        const expected = { meetingDate: currentDate, createdDate: currentDate, editedDate: currentDate, ...returnedFromService };

        axiosStub.post.resolves({ data: returnedFromService });
        return service.create({}).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not create a MeetingTranscript', async () => {
        axiosStub.post.rejects(error);

        return service
          .create({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should update a MeetingTranscript', async () => {
        const returnedFromService = {
          title: 'BBBBBB',
          meetingDate: dayjs(currentDate).format(DATE_FORMAT),
          attendees: 'BBBBBB',
          body: 'BBBBBB',
          source: 'BBBBBB',
          createdDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          editedDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...elemDefault,
        };

        const expected = { meetingDate: currentDate, createdDate: currentDate, editedDate: currentDate, ...returnedFromService };
        axiosStub.put.resolves({ data: returnedFromService });

        return service.update(expected).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not update a MeetingTranscript', async () => {
        axiosStub.put.rejects(error);

        return service
          .update({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should partial update a MeetingTranscript', async () => {
        const patchObject = {
          title: 'BBBBBB',
          meetingDate: dayjs(currentDate).format(DATE_FORMAT),
          attendees: 'BBBBBB',
          body: 'BBBBBB',
          createdDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          editedDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...new MeetingTranscript(),
        };
        const returnedFromService = Object.assign(patchObject, elemDefault);

        const expected = { meetingDate: currentDate, createdDate: currentDate, editedDate: currentDate, ...returnedFromService };
        axiosStub.patch.resolves({ data: returnedFromService });

        return service.partialUpdate(patchObject).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not partial update a MeetingTranscript', async () => {
        axiosStub.patch.rejects(error);

        return service
          .partialUpdate({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should return a list of MeetingTranscript', async () => {
        const returnedFromService = {
          title: 'BBBBBB',
          meetingDate: dayjs(currentDate).format(DATE_FORMAT),
          attendees: 'BBBBBB',
          body: 'BBBBBB',
          source: 'BBBBBB',
          createdDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          editedDate: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...elemDefault,
        };
        const expected = { meetingDate: currentDate, createdDate: currentDate, editedDate: currentDate, ...returnedFromService };
        axiosStub.get.resolves([returnedFromService]);
        return service.retrieve({ sort: {}, page: 0, size: 10 }).then(res => {
          expect(res).toContainEqual(expected);
        });
      });

      it('should not return a list of MeetingTranscript', async () => {
        axiosStub.get.rejects(error);

        return service
          .retrieve()
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should delete a MeetingTranscript', async () => {
        axiosStub.delete.resolves({ ok: true });
        return service.delete(123).then(res => {
          expect(res.ok).toBeTruthy();
        });
      });

      it('should not delete a MeetingTranscript', async () => {
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
