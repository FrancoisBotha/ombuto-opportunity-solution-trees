import axios from 'axios';

export interface BackupRestoreSummary {
  exportedAt: string;
  counts: Record<string, number>;
}

const baseApiUrl = 'api/admin/backup';

export default class BackupService {
  public download() {
    return axios.get<Blob>(baseApiUrl, { responseType: 'blob' });
  }

  public restore(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return axios.post<BackupRestoreSummary>(`${baseApiUrl}/restore`, formData);
  }
}
