const API_PREFIX = '/api/v1';
const AUTH_TOKEN_KEY = 'auth_token';
const AUTH_REFRESH_TOKEN_KEY = 'auth_refresh_token';
const AUTH_TOKEN_TYPE_KEY = 'auth_token_type';

let refreshInFlight = null;

const isPublicPath = (path) =>
  path.startsWith('/auth/login') || path.startsWith('/auth/refresh') || path.startsWith('/users/register');

const resolveUrl = (path) => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_PREFIX}${normalizedPath}`;
};

const parseResponseBody = async (response) => {
  const contentType = response.headers.get('content-type') || '';

  if (!contentType.includes('application/json')) {
    return null;
  }

  return response.json();
};

const buildApiError = (response, body) => {
  const apiError = body?.error;
  const error = new Error(apiError?.message || `Falha na requisição (${response.status})`);

  error.status = response.status;
  error.code = apiError?.code;
  error.field = apiError?.field;

  return error;
};

const persistTokens = (tokenData) => {
  if (!tokenData) {
    return;
  }

  if (tokenData.accessToken) {
    localStorage.setItem(AUTH_TOKEN_KEY, tokenData.accessToken);
  }

  if (tokenData.refreshToken) {
    localStorage.setItem(AUTH_REFRESH_TOKEN_KEY, tokenData.refreshToken);
  }

  if (tokenData.tokenType) {
    localStorage.setItem(AUTH_TOKEN_TYPE_KEY, tokenData.tokenType);
  }
};

const clearPersistedTokens = () => {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_REFRESH_TOKEN_KEY);
  localStorage.removeItem(AUTH_TOKEN_TYPE_KEY);
};

const tryRefreshToken = async () => {
  if (refreshInFlight) {
    return refreshInFlight;
  }

  const storedRefreshToken = localStorage.getItem(AUTH_REFRESH_TOKEN_KEY);

  if (!storedRefreshToken) {
    return null;
  }

  refreshInFlight = (async () => {
    const response = await fetch(resolveUrl('/auth/refresh'), {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken: storedRefreshToken }),
    });

    const payload = await parseResponseBody(response);

    if (!response.ok) {
      clearPersistedTokens();
      return null;
    }

    const tokenData = payload?.data;

    if (!tokenData?.accessToken) {
      clearPersistedTokens();
      return null;
    }

    persistTokens(tokenData);
    return tokenData.accessToken;
  })().finally(() => {
    refreshInFlight = null;
  });

  return refreshInFlight;
};

async function http(method, path, body, extraHeaders = {}, options = { retryOnUnauthorized: true }) {
  const headers = {
    Accept: 'application/json',
    ...extraHeaders,
  };

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  if (!isPublicPath(path)) {
    const storedToken = localStorage.getItem(AUTH_TOKEN_KEY);
    if (storedToken) {
      headers.Authorization = `Bearer ${storedToken}`;
    }
  }

  const response = await fetch(resolveUrl(path), {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const payload = await parseResponseBody(response);

  if (
    response.status === 401 &&
    options.retryOnUnauthorized &&
    !isPublicPath(path)
  ) {
    const refreshedAccessToken = await tryRefreshToken();

    if (refreshedAccessToken) {
      return http(method, path, body, extraHeaders, { retryOnUnauthorized: false });
    }
  }

  if (!response.ok) {
    throw buildApiError(response, payload);
  }

  return payload;
}

async function httpMultipart(path, formData, options = { retryOnUnauthorized: true }) {
  const headers = {
    Accept: 'application/json',
  };

  if (!isPublicPath(path)) {
    const storedToken = localStorage.getItem(AUTH_TOKEN_KEY);
    if (storedToken) {
      headers.Authorization = `Bearer ${storedToken}`;
    }
  }

  const response = await fetch(resolveUrl(path), {
    method: 'POST',
    headers,
    body: formData,
  });

  const payload = await parseResponseBody(response);

  if (response.status === 401 && options.retryOnUnauthorized && !isPublicPath(path)) {
    const refreshedAccessToken = await tryRefreshToken();

    if (refreshedAccessToken) {
      return httpMultipart(path, formData, { retryOnUnauthorized: false });
    }
  }

  if (!response.ok) {
    throw buildApiError(response, payload);
  }

  return payload;
}

export const api = {
  auth: {
    login: (credentials) => http('POST', '/auth/login', credentials).then((envelope) => envelope?.data ?? null),
    refresh: (refreshToken) =>
      http('POST', '/auth/refresh', { refreshToken }).then((envelope) => envelope?.data ?? null),
    logout: () => http('POST', '/auth/logout').then((envelope) => envelope?.data ?? null),
  },
  users: {
    register: (payload) => http('POST', '/users/register', payload).then((envelope) => envelope?.data ?? null),
    me: () => http('GET', '/users/me').then((envelope) => envelope?.data ?? null),
  },
  projects: {
    create: (name) => http('POST', '/projects', { name }).then((envelope) => envelope?.data ?? null),
    list: (includeArchived = false) =>
      http('GET', `/projects?includeArchived=${includeArchived}`).then((envelope) => envelope?.data ?? []),
    getById: (projectId) => http('GET', `/projects/${projectId}`).then((envelope) => envelope?.data ?? null),
  },
  reports: {
    metrics: (projectId) => http('GET', `/projects/${projectId}/metrics`).then((envelope) => envelope?.data ?? null),
  },
  defects: {
    list: (projectId) => http('GET', `/projects/${projectId}/defects`).then((envelope) => envelope?.data ?? []),
  },
  requirements: {
    create: (projectId, title) =>
      http('POST', `/projects/${projectId}/requirements`, { title }).then((envelope) => envelope?.data ?? null),
    list: (projectId) => http('GET', `/projects/${projectId}/requirements`).then((envelope) => envelope?.data ?? []),
  },
  testCases: {
    create: (projectId, payload) =>
      http('POST', `/projects/${projectId}/test-cases`, payload).then((envelope) => envelope?.data ?? null),
    list: (projectId) => http('GET', `/projects/${projectId}/test-cases`).then((envelope) => envelope?.data ?? []),
    getById: (projectId, testCaseId) =>
      http('GET', `/projects/${projectId}/test-cases/${testCaseId}`).then((envelope) => envelope?.data ?? null),
    update: (projectId, testCaseId, payload) =>
      http('PATCH', `/projects/${projectId}/test-cases/${testCaseId}`, payload).then((envelope) => envelope?.data ?? null),
    remove: (projectId, testCaseId) =>
      http('DELETE', `/projects/${projectId}/test-cases/${testCaseId}`).then((envelope) => envelope?.data ?? null),
    uploadAttachment: (projectId, testCaseId, file) => {
      const formData = new FormData();
      formData.append('file', file);

      return httpMultipart(`/projects/${projectId}/test-cases/${testCaseId}/attachments`, formData).then(
        (envelope) => envelope?.data ?? null,
      );
    },
    search: (projectId, query) =>
      http('GET', `/projects/${projectId}/test-cases/search?query=${encodeURIComponent(query)}`).then(
        (envelope) => envelope?.data ?? [],
      ),
  },
  suites: {
    create: (projectId, name) =>
      http('POST', `/projects/${projectId}/suites`, { name }).then((envelope) => envelope?.data ?? null),
    tree: (projectId) => http('GET', `/projects/${projectId}/suites/tree`).then((envelope) => envelope?.data ?? []),
  },
  admin: {
    auditLogs: () => http('GET', '/admin/audit-logs').then((envelope) => envelope?.data ?? []),
  },
  session: {
    persistTokens,
    clearPersistedTokens,
  },
};