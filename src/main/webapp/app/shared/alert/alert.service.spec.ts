import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { MESSAGE_ERROR_HEADER_NAME, MESSAGE_PARAM_HEADER_NAME } from '@/shared/jhipster/constants';

import AlertService from './alert.service';

describe('Alert Service test suite', () => {
  let toastStub: vitest.Mock;
  let alertService: AlertService;

  beforeEach(() => {
    toastStub = vitest.fn();
    alertService = new AlertService({
      toast: {
        show: toastStub,
      } as any,
    });
  });

  it('should show error toast with translation/message', () => {
    const message = 'translatedMessage';

    // WHEN
    alertService.showError(message);

    // THEN
    expect(toastStub).toHaveBeenCalledExactlyOnceWith({
      props: {
        body: message,
        pos: 'top-center',
        title: 'Error',
        variant: 'danger',
        solid: true,
      },
    });
  });

  it('should show not reachable toast when http status = 0', () => {
    const message = 'Server not reachable';
    const httpErrorResponse = {
      status: 0,
    };

    // WHEN
    alertService.showHttpError(httpErrorResponse);

    // THEN
    expect(toastStub).toHaveBeenCalledExactlyOnceWith({
      props: {
        body: expect.any(String),
        pos: 'top-center',
        solid: true,
        title: 'Error',
        variant: 'danger',
      },
    });
  });

  it('should show parameterized error toast when http status = 400 and entity headers', () => {
    const message = 'Updation Error';
    const httpErrorResponse = {
      status: 400,
      headers: {
        [MESSAGE_ERROR_HEADER_NAME]: message,
        [MESSAGE_PARAM_HEADER_NAME]: 'dummyEntity',
      },
    };

    // WHEN
    alertService.showHttpError(httpErrorResponse);

    // THEN
    expect(toastStub).toHaveBeenCalledWith({
      props: {
        body: expect.any(String),
        pos: 'top-center',
        solid: true,
        title: 'Error',
        variant: 'danger',
      },
    });
  });

  it('should show error toast with data.message when http status = 400 and entity headers', () => {
    const message = 'Validation error';
    const httpErrorResponse = {
      status: 400,
      headers: {
        [`${MESSAGE_ERROR_HEADER_NAME}400`]: 'error',
        [`${MESSAGE_PARAM_HEADER_NAME}400`]: 'dummyEntity',
      },
      data: {
        message,
        fieldErrors: {
          field1: 'error1',
        },
      },
    };

    // WHEN
    alertService.showHttpError(httpErrorResponse);

    // THEN
    expect(toastStub).toHaveBeenCalledExactlyOnceWith({
      props: {
        body: expect.any(String),
        pos: 'top-center',
        solid: true,
        title: 'Error',
        variant: 'danger',
      },
    });
  });

  it('should show error toast when http status = 404', () => {
    const message = 'The page does not exist.';
    const httpErrorResponse = {
      status: 404,
    };

    // WHEN
    alertService.showHttpError(httpErrorResponse);

    // THEN
    expect(toastStub).toHaveBeenCalledExactlyOnceWith({
      props: {
        body: expect.any(String),
        pos: 'top-center',
        solid: true,
        title: 'Error',
        variant: 'danger',
      },
    });
  });

  it('should show error toast when http status != 400,404', () => {
    const message = 'Error 500';
    const httpErrorResponse = {
      status: 500,
      data: {
        message,
      },
    };

    // WHEN
    alertService.showHttpError(httpErrorResponse);

    // THEN
    expect(toastStub).toHaveBeenCalledExactlyOnceWith({
      props: {
        body: expect.any(String),
        pos: 'top-center',
        solid: true,
        title: 'Error',
        variant: 'danger',
      },
    });
  });
});
