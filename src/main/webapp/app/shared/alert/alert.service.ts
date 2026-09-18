import { type BToastProps, useToast } from 'bootstrap-vue-next';

import { getMessageFromHeaders } from '@/shared/jhipster/headers';

export const useAlertService = () => {
  const toast = useToast();
  if (!toast) {
    throw new Error('BootstrapVue toast component was not found');
  }
  return new AlertService({
    toast,
  });
};

export default class AlertService {
  private readonly toast: ReturnType<typeof useToast>;

  constructor({ toast }: { toast: ReturnType<typeof useToast> }) {
    this.toast = toast;
  }

  showInfo(toastMessage: string, props: BToastProps = {}) {
    this.toast.show!({
      props: {
        pos: 'top-center',
        title: 'Info',
        variant: 'info',
        solid: true,
        body: toastMessage,
        ...props,
      },
    });
  }

  showSuccess(toastMessage: string) {
    this.showInfo(toastMessage, {
      title: 'Success',
      variant: 'success',
    });
  }

  showError(toastMessage: string) {
    this.showInfo(toastMessage, {
      title: 'Error',
      variant: 'danger',
    });
  }

  showHttpError({ data, status, headers }: any) {
    let errorMessage: string | null = null;
    switch (status) {
      case 0:
        errorMessage = 'Server not reachable';
        break;

      case 400: {
        const message = getMessageFromHeaders(headers);
        if (message.errorMessage) {
          errorMessage = message.errorMessage;
        } else if (data.message) {
          errorMessage = data.message;
        } else if (data?.fieldErrors) {
          errorMessage = 'Validation error';
        }
        break;
      }

      case 404:
        errorMessage = 'The page does not exist.';
        break;

      default:
        errorMessage = data.message;
    }
    this.showError(errorMessage!);
  }
}
