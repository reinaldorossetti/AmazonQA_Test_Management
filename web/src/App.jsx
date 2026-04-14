import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Navigate, NavLink, Route, Routes, useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  AppBar,
  Avatar,
  Box,
  Button,
  CircularProgress,
  Container,
  Divider,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  LinearProgress,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Tab,
  Tabs,
  Paper,
  Stack,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import AppsRoundedIcon from '@mui/icons-material/AppsRounded';
import NotificationsNoneRoundedIcon from '@mui/icons-material/NotificationsNoneRounded';
import SettingsRoundedIcon from '@mui/icons-material/SettingsRounded';
import TaskAltRoundedIcon from '@mui/icons-material/TaskAltRounded';
import ArticleRoundedIcon from '@mui/icons-material/ArticleRounded';
import MoreHorizRoundedIcon from '@mui/icons-material/MoreHorizRounded';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import LinkRoundedIcon from '@mui/icons-material/LinkRounded';
import PersonAddAlt1RoundedIcon from '@mui/icons-material/PersonAddAlt1Rounded';
import FolderOpenRoundedIcon from '@mui/icons-material/FolderOpenRounded';
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined';
import FilterListRoundedIcon from '@mui/icons-material/FilterListRounded';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import ArrowOutwardRoundedIcon from '@mui/icons-material/ArrowOutwardRounded';
import PersonOutlineRoundedIcon from '@mui/icons-material/PersonOutlineRounded';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded';
import FormatBoldRoundedIcon from '@mui/icons-material/FormatBoldRounded';
import FormatItalicRoundedIcon from '@mui/icons-material/FormatItalicRounded';
import FormatUnderlinedRoundedIcon from '@mui/icons-material/FormatUnderlinedRounded';
import StrikethroughSRoundedIcon from '@mui/icons-material/StrikethroughSRounded';
import FormatQuoteRoundedIcon from '@mui/icons-material/FormatQuoteRounded';
import FormatListBulletedRoundedIcon from '@mui/icons-material/FormatListBulletedRounded';
import FormatListNumberedRoundedIcon from '@mui/icons-material/FormatListNumberedRounded';
import InsertLinkRoundedIcon from '@mui/icons-material/InsertLinkRounded';
import SentimentSatisfiedAltRoundedIcon from '@mui/icons-material/SentimentSatisfiedAltRounded';
import ImageRoundedIcon from '@mui/icons-material/ImageRounded';
import VideocamRoundedIcon from '@mui/icons-material/VideocamRounded';
import CodeRoundedIcon from '@mui/icons-material/CodeRounded';
import TableChartRoundedIcon from '@mui/icons-material/TableChartRounded';
import { useAuth } from './contexts/AuthContext';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { LoginPage } from './components/auth/LoginPage';
import { RegisterPage } from './components/auth/RegisterPage';
import { api } from './db/api';

const EMPTY_METRICS = {
  totalExecutions: 0,
  passed: 0,
  failed: 0,
  blocked: 0,
  notRun: 0,
};

const toDisplayName = (rawName) => {
  if (!rawName) {
    return 'Usuário';
  }

  const base = rawName.includes('@') ? rawName.split('@')[0] : rawName;

  return base
    .replace(/[._-]+/g, ' ')
    .trim()
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0].toUpperCase() + part.slice(1))
    .join(' ');
};

const getGreeting = () => {
  const hour = new Date().getHours();

  if (hour < 12) {
    return 'Good morning';
  }

  if (hour < 18) {
    return 'Good afternoon';
  }

  return 'Good evening';
};

const statusToProgress = (executionStatus, metrics) => {
  if (!executionStatus) {
    if ((metrics?.totalExecutions ?? 0) === 0) {
      return 0;
    }

    return Math.round(((metrics?.passed ?? 0) / (metrics?.totalExecutions ?? 1)) * 100);
  }

  const normalized = executionStatus.toString().toUpperCase().replace(/\s+/g, '_');

  switch (normalized) {
    case 'PASSED':
      return 100;
    case 'FAILED':
      return 77;
    case 'BLOCKED':
      return 35;
    case 'IN_PROGRESS':
      return 52;
    case 'NOT_RUN':
      return 0;
    default:
      return 0;
  }
};

const inferFolderName = (title = '') => {
  const lower = title.toLowerCase();

  if (lower.includes('login')) {
    return 'Login';
  }

  if (lower.includes('cadastro') || lower.includes('registro')) {
    return 'Cadastro';
  }

  if (lower.includes('carrinho') || lower.includes('checkout')) {
    return 'Carrinho de Compras';
  }

  return 'General';
};

const splitExpectedResult = (expectedResult) => {
  if (!expectedResult) {
    return ['Deve validar fluxo esperado do cenário.'];
  }

  return expectedResult
    .split(/\n|;|\./)
    .map((item) => item.trim())
    .filter(Boolean);
};

  const MAX_ATTACHMENT_SIZE_BYTES = 1_048_576;
  const ALLOWED_ATTACHMENT_EXTENSIONS = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'xlsx', 'xls', 'csv'];

const richTextToPlain = (value = '') => {
  if (!value) {
    return '';
  }

  if (typeof document === 'undefined') {
    return value
      .replace(/<br\s*\/?\s*>/gi, '\n')
      .replace(/<\/(p|div|li|blockquote|h[1-6])>/gi, '\n')
      .replace(/<li>/gi, '- ')
      .replace(/<[^>]+>/g, ' ')
      .replace(/\r\n/g, '\n')
      .replace(/\n{3,}/g, '\n\n')
      .replace(/[ \t]+\n/g, '\n')
      .trim();
  }

  const element = document.createElement('div');
  element.innerHTML = value;

  element.querySelectorAll('br').forEach((br) => {
    br.replaceWith(document.createTextNode('\n'));
  });

  element.querySelectorAll('p, div, li, blockquote').forEach((node) => {
    node.appendChild(document.createTextNode('\n'));
  });

  return (element.textContent || element.innerText || '')
    .replace(/\u00a0/g, ' ')
    .replace(/\r\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .replace(/[ \t]+\n/g, '\n')
    .trim();
};

const escapeHtml = (value = '') =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

const toEditorHtml = (value = '') => {
  if (!value) {
    return '';
  }

  const hasHtml = /<\/?[a-z][\s\S]*>/i.test(value);
  if (hasHtml) {
    return value;
  }

  return escapeHtml(value).replace(/\r\n/g, '\n').replace(/\n/g, '<br>');
};

const isAllowedAttachmentFile = (file) => {
  if (!file) {
    return false;
  }

  const extension = (file.name.split('.').pop() || '').toLowerCase();
  const isImage = (file.type || '').startsWith('image/') || ['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(extension);
  const isSpreadsheet = ['xlsx', 'xls', 'csv'].includes(extension);

  return isImage || isSpreadsheet;
};

const normalizeTestCaseStatusValue = (value) => {
  const input = (value || '').toString().trim();
  if (!input) {
    return 'Draft';
  }

  const lookup = input.toLowerCase().replace(/[\s_-]+/g, ' ');
  const map = {
    draft: 'Draft',
    ready: 'Ready for Review',
    'ready for review': 'Ready for Review',
    'review in progress': 'Review in Progress',
    rework: 'Rework',
    final: 'Final',
    future: 'Future',
    obsolete: 'Obsolete',
  };

  return map[lookup] || input;
};

function TopBar({ displayName, projectName, projectId }) {
  const navItemStyle = ({ isActive }) => ({
    color: isActive ? '#4f46e5' : '#6b7280',
    textDecoration: 'none',
    fontWeight: isActive ? 700 : 600,
    fontSize: 14,
    padding: '18px 2px',
    borderBottom: isActive ? '2px solid #4f46e5' : '2px solid transparent',
  });

  return (
    <AppBar
      position="sticky"
      color="inherit"
      elevation={0}
      sx={{ borderBottom: '1px solid', borderColor: '#e5e7eb', bgcolor: '#fff' }}
    >
      <Toolbar sx={{ minHeight: 64, justifyContent: 'space-between', gap: 2 }}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ minWidth: 300 }}>
          <AppsRoundedIcon sx={{ color: '#5b4dfa', fontSize: 20 }} />
          <Typography variant="h5" fontWeight={800} sx={{ color: '#111827', fontSize: 34, lineHeight: 1 }}>
            QA Sphere
          </Typography>
          {projectName ? (
            <>
              <Divider orientation="vertical" flexItem sx={{ mx: 1 }} />
              <Typography color="text.secondary" fontWeight={500}>
                {projectName}
              </Typography>
            </>
          ) : null}
        </Stack>

        {projectId ? (
          <Stack direction="row" spacing={3} alignItems="center" sx={{ flexGrow: 1, justifyContent: 'center' }}>
            <NavLink to={`/projects/${projectId}/overview`} style={navItemStyle}>
              Overview
            </NavLink>
            <Box sx={{ color: '#6b7280', fontWeight: 600, fontSize: 14, py: 2.25 }}>Test Runs</Box>
            <NavLink to={`/projects/${projectId}/test-cases`} style={navItemStyle}>
              Test Cases
            </NavLink>
            <Box sx={{ color: '#6b7280', fontWeight: 600, fontSize: 14, py: 2.25 }}>Reports</Box>
          </Stack>
        ) : (
          <Box sx={{ flexGrow: 1 }} />
        )}

        <Stack direction="row" spacing={0.8} alignItems="center">
          <IconButton size="small">
            <NotificationsNoneRoundedIcon />
          </IconButton>
          <IconButton size="small">
            <SettingsRoundedIcon />
          </IconButton>
          <Avatar sx={{ width: 36, height: 36 }}>{displayName[0] || 'U'}</Avatar>
        </Stack>
      </Toolbar>
    </AppBar>
  );
}

function AssignedRunCard({ run }) {
  return (
    <Paper
      elevation={0}
      sx={{
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
        p: 1.8,
      }}
    >
      <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={2}>
        <Box sx={{ minWidth: 0 }}>
          <Typography fontWeight={600} noWrap>
            {run.title}
          </Typography>
          <Typography variant="body2" color="text.secondary" noWrap>
            {run.projectName}
          </Typography>
        </Box>

        <Stack direction="row" spacing={1.4} alignItems="center" sx={{ minWidth: 220 }}>
          <Box sx={{ flexGrow: 1 }}>
            <LinearProgress
              variant="determinate"
              value={run.progress}
              sx={{
                height: 6,
                borderRadius: 99,
                bgcolor: '#eef2ff',
              }}
              color={run.progress >= 80 ? 'success' : run.progress > 0 ? 'primary' : 'inherit'}
            />
          </Box>

          <Avatar sx={{ width: 24, height: 24, fontSize: 12 }}>{run.ownerInitial}</Avatar>

          <Stack direction="row" spacing={0.6} alignItems="center" sx={{ minWidth: 52 }}>
            <CircularProgress variant="determinate" value={run.progress} size={24} thickness={5} />
            <Typography variant="caption" color="text.secondary" fontWeight={600}>
              {run.progress}%
            </Typography>
          </Stack>
        </Stack>
      </Stack>
    </Paper>
  );
}

function ProjectCard({ project, onOpen }) {
  return (
    <Paper
      elevation={0}
      sx={{
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
        p: 2.2,
        height: '100%',
        cursor: 'pointer',
        transition: 'all 0.2s ease',
        '&:hover': {
          borderColor: '#c7d2fe',
          boxShadow: '0 6px 16px rgba(79,70,229,0.10)',
          transform: 'translateY(-1px)',
        },
      }}
      onClick={onOpen}
    >
      <Stack spacing={1.5}>
        <Typography variant="h6" fontWeight={700}>
          {project.name}
        </Typography>

        <Stack direction="row" spacing={1} alignItems="center" color="text.secondary">
          <ArticleRoundedIcon sx={{ fontSize: 18 }} />
          <Typography variant="body2">{project.totalExecutions} test runs</Typography>
        </Stack>

        <Stack direction="row" spacing={1} alignItems="center" color="text.secondary">
          <TaskAltRoundedIcon sx={{ fontSize: 18 }} />
          <Typography variant="body2">{project.testCaseCount} test cases</Typography>
        </Stack>
      </Stack>
    </Paper>
  );
}

function HomePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [projectInsights, setProjectInsights] = useState([]);
  const [assignedRuns, setAssignedRuns] = useState([]);
  const [dashboardError, setDashboardError] = useState('');
  const [isLoadingDashboard, setIsLoadingDashboard] = useState(true);
  const [isCreateProjectOpen, setIsCreateProjectOpen] = useState(false);
  const [projectNameInput, setProjectNameInput] = useState('');
  const [projectCreateFeedback, setProjectCreateFeedback] = useState('');
  const [isCreatingProject, setIsCreatingProject] = useState(false);

  const loadDashboard = useCallback(async () => {
    setIsLoadingDashboard(true);
    setDashboardError('');

    try {
      const projects = await api.projects.list();

      const enriched = await Promise.all(
        (projects || []).map(async (project) => {
          try {
            const [metrics, testCases] = await Promise.all([
              api.reports.metrics(project.id),
              api.testCases.list(project.id),
            ]);

            return {
              ...project,
              metrics: metrics || EMPTY_METRICS,
              testCases: testCases || [],
              totalExecutions: metrics?.totalExecutions ?? 0,
              testCaseCount: (testCases || []).length,
            };
          } catch {
            return {
              ...project,
              metrics: EMPTY_METRICS,
              testCases: [],
              totalExecutions: 0,
              testCaseCount: 0,
            };
          }
        }),
      );

      setProjectInsights(enriched);

      const runs = enriched
        .flatMap((project) => {
          const cards = (project.testCases || []).slice(0, 3).map((testCase) => ({
            id: testCase.id,
            title: testCase.title,
            projectName: project.name,
            progress: statusToProgress(testCase.executionStatus, project.metrics),
            ownerInitial: toDisplayName(user?.username)?.[0] || 'U',
          }));

          if (cards.length > 0) {
            return cards;
          }

          return [
            {
              id: `${project.id}-fallback-run`,
              title: 'Functional Testing',
              projectName: project.name,
              progress: statusToProgress(null, project.metrics),
              ownerInitial: toDisplayName(user?.username)?.[0] || 'U',
            },
          ];
        })
        .slice(0, 6);

      setAssignedRuns(runs);
    } catch (error) {
      setDashboardError(error.message || 'Falha ao carregar dashboard.');
      setProjectInsights([]);
      setAssignedRuns([]);
    } finally {
      setIsLoadingDashboard(false);
    }
  }, [user?.username]);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const displayName = useMemo(() => toDisplayName(user?.username), [user?.username]);

  const handleCreateProject = async () => {
    const cleanName = projectNameInput.trim();

    if (!cleanName) {
      setProjectCreateFeedback('Informe um nome para o projeto.');
      return;
    }

    setIsCreatingProject(true);
    setProjectCreateFeedback('');

    try {
      await api.projects.create(cleanName);
      setProjectNameInput('');
      setIsCreateProjectOpen(false);
      await loadDashboard();
    } catch (error) {
      if (error?.status === 403) {
        setProjectCreateFeedback('Seu perfil atual não possui permissão para criar projeto. Use ADMIN ou LEADER.');
      } else {
        setProjectCreateFeedback(error.message || 'Falha ao criar projeto.');
      }
    } finally {
      setIsCreatingProject(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#ffffff' }}>
      <TopBar displayName={displayName} />

      <Container maxWidth={false} sx={{ px: { xs: 2, md: 2.5 }, py: 2.2 }}>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={1.6}
          alignItems={{ xs: 'flex-start', md: 'center' }}
          justifyContent="space-between"
          sx={{ mb: 2.2 }}
        >
          <Typography variant="h4" fontWeight={700} sx={{ fontSize: { xs: 28, md: 40 } }}>
            {getGreeting()}, {displayName}
          </Typography>

          <Stack direction="row" spacing={1}>
            <Button
              variant="contained"
              onClick={() => {
                setProjectCreateFeedback('');
                setIsCreateProjectOpen(true);
              }}
              sx={{ bgcolor: '#111827', '&:hover': { bgcolor: '#0b1220' } }}
            >
              Add Project
            </Button>
            <Button variant="outlined" onClick={logout}>Sair</Button>
          </Stack>
        </Stack>

        <Divider sx={{ mb: 2.2 }} />

        {dashboardError ? (
          <Alert severity="error" sx={{ mb: 2.2 }}>
            {dashboardError}
          </Alert>
        ) : null}

        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" fontWeight={700} sx={{ mb: 1.4 }}>
            Assigned Test Runs
          </Typography>

          {isLoadingDashboard ? (
            <Stack alignItems="center" py={4} spacing={1}>
              <CircularProgress size={24} />
              <Typography variant="body2" color="text.secondary">
                Carregando dashboard...
              </Typography>
            </Stack>
          ) : assignedRuns.length > 0 ? (
            <Grid container spacing={1.4}>
              {assignedRuns.map((run) => (
                <Grid item xs={12} md={6} key={run.id}>
                  <AssignedRunCard run={run} />
                </Grid>
              ))}
            </Grid>
          ) : (
            <Paper elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Nenhum test run atribuído no momento.
              </Typography>
            </Paper>
          )}
        </Box>

        <Divider sx={{ mb: 2.2 }} />

        <Box>
          <Typography variant="h6" fontWeight={700} sx={{ mb: 1.4 }}>
            Projects
          </Typography>

          {projectInsights.length > 0 ? (
            <Grid container spacing={1.6}>
              {projectInsights.map((project) => (
                <Grid item xs={12} md={6} key={project.id}>
                  <ProjectCard
                    project={project}
                    onOpen={() => navigate(`/projects/${project.id}/overview`)}
                  />
                </Grid>
              ))}
            </Grid>
          ) : (
            <Paper elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Ainda não existem projetos visíveis para este usuário.
              </Typography>
            </Paper>
          )}
        </Box>
      </Container>

      <Dialog open={isCreateProjectOpen} onClose={() => setIsCreateProjectOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Add Project</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ pt: 0.8 }}>
            {projectCreateFeedback ? <Alert severity="error">{projectCreateFeedback}</Alert> : null}
            <TextField
              label="Project name"
              value={projectNameInput}
              onChange={(event) => setProjectNameInput(event.target.value)}
              autoFocus
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsCreateProjectOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateProject} disabled={isCreatingProject}>
            {isCreatingProject ? 'Creating...' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

function ProjectOverviewPage() {
  const { projectId } = useParams();
  const { user } = useAuth();

  const displayName = useMemo(() => toDisplayName(user?.username), [user?.username]);

  const [project, setProject] = useState(null);
  const [metrics, setMetrics] = useState(EMPTY_METRICS);
  const [testCases, setTestCases] = useState([]);
  const [overviewError, setOverviewError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isCancelled = false;

    const loadOverview = async () => {
      if (!projectId) {
        return;
      }

      setIsLoading(true);
      setOverviewError('');

      try {
        const [projectData, metricsData, testCaseData] = await Promise.all([
          api.projects.getById(projectId),
          api.reports.metrics(projectId),
          api.testCases.list(projectId),
        ]);

        if (isCancelled) {
          return;
        }

        setProject(projectData);
        setMetrics(metricsData || EMPTY_METRICS);
        setTestCases(testCaseData || []);
      } catch (error) {
        if (!isCancelled) {
          setOverviewError(error.message || 'Não foi possível carregar o overview do projeto.');
          setProject(null);
          setMetrics(EMPTY_METRICS);
          setTestCases([]);
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    loadOverview();

    return () => {
      isCancelled = true;
    };
  }, [projectId]);

  const recentRuns = useMemo(() => {
    if (testCases.length === 0) {
      return [
        { id: 'placeholder-run-1', title: 'e.g. New Version Full Test', progress: 0 },
        { id: 'placeholder-run-2', title: 'e.g. Test New Checkout UI', progress: 0 },
      ];
    }

    return testCases.slice(0, 2).map((testCase) => ({
      id: testCase.id,
      title: testCase.title,
      progress: statusToProgress(testCase.executionStatus, metrics),
      ownerInitial: displayName[0] || 'U',
    }));
  }, [testCases, metrics, displayName]);

  const recentCases = useMemo(() => {
    return testCases.slice(0, 5);
  }, [testCases]);

  const lastTestCaseUpdated = useMemo(() => {
    if (recentCases.length === 0) {
      return '--';
    }

    return 'recently';
  }, [recentCases]);

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#fff' }}>
      <TopBar displayName={displayName} projectName={project?.name || 'Project'} projectId={projectId} />

      <Container maxWidth={false} sx={{ px: { xs: 2, md: 2 }, py: 2.2 }}>
        {overviewError ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {overviewError}
          </Alert>
        ) : null}

        {isLoading ? (
          <Stack alignItems="center" py={8} spacing={1}>
            <CircularProgress size={24} />
            <Typography variant="body2" color="text.secondary">
              Carregando Overview...
            </Typography>
          </Stack>
        ) : (
          <>
            <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1.5 }}>
              <Typography variant="h5" fontWeight={700}>Overview</Typography>
              <IconButton size="small" sx={{ border: '1px solid', borderColor: 'divider' }}>
                <MoreHorizRoundedIcon />
              </IconButton>
            </Stack>

            <Divider sx={{ mb: 2.3 }} />

            <Grid container spacing={1.8} sx={{ mb: 2.2 }}>
              <Grid item xs={12} sm={6} md={3}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                  <Typography variant="body2" color="text.secondary">Last Test Run Created</Typography>
                  <Typography fontWeight={700} mt={0.8}>--</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                  <Typography variant="body2" color="text.secondary">Number of Test Runs</Typography>
                  <Typography fontWeight={700} mt={0.8}>{metrics.totalExecutions ?? 0}</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                  <Typography variant="body2" color="text.secondary">Last Test Case Updated</Typography>
                  <Typography fontWeight={700} mt={0.8}>{lastTestCaseUpdated}</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                  <Typography variant="body2" color="text.secondary">Number of Test Cases</Typography>
                  <Typography fontWeight={700} mt={0.8}>{testCases.length}</Typography>
                </Paper>
              </Grid>
            </Grid>

            <Grid container spacing={2}>
              <Grid item xs={12} lg={9}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2, mb: 2 }}>
                  <Typography variant="h6" fontWeight={700} sx={{ mb: 1.2 }}>Recent Test Runs</Typography>
                  <Stack spacing={1.2}>
                    {recentRuns.map((run) => (
                      <Paper key={run.id} elevation={0} sx={{ p: 1.4, border: '1px solid', borderColor: '#e5e7eb', borderRadius: 1.5 }}>
                        <Stack direction="row" alignItems="center" spacing={1.5} justifyContent="space-between">
                          <Typography color="text.secondary" noWrap>{run.title}</Typography>

                          <Stack direction="row" alignItems="center" spacing={1.2} sx={{ minWidth: 220 }}>
                            <LinearProgress
                              variant="determinate"
                              value={run.progress}
                              sx={{
                                flexGrow: 1,
                                height: 6,
                                borderRadius: 999,
                                bgcolor: '#eef2ff',
                              }}
                            />
                            <Avatar sx={{ width: 22, height: 22, fontSize: 11 }}>{run.ownerInitial || displayName[0] || 'U'}</Avatar>
                            <Stack direction="row" spacing={0.5} alignItems="center">
                              <CircularProgress size={20} thickness={5} variant="determinate" value={run.progress} />
                              <Typography variant="caption" color="text.secondary">{run.progress}%</Typography>
                            </Stack>
                          </Stack>
                        </Stack>
                      </Paper>
                    ))}
                  </Stack>
                </Paper>

                <Paper elevation={0} sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                  <Typography variant="h6" fontWeight={700} sx={{ mb: 1.2 }}>Recent Test Cases</Typography>

                  {recentCases.length > 0 ? (
                    <List dense sx={{ p: 0 }}>
                      {recentCases.map((testCase) => (
                        <ListItem
                          key={testCase.id}
                          sx={{
                            borderTop: '1px solid',
                            borderColor: '#f3f4f6',
                            px: 1,
                            '&:first-of-type': { borderTop: 'none' },
                          }}
                        >
                          <ListItemText
                            primary={testCase.title}
                            secondary={testCase.testId || 'Sem TestID'}
                            primaryTypographyProps={{ fontSize: 15 }}
                            secondaryTypographyProps={{ fontSize: 12 }}
                          />
                          <Typography variant="body2" color="text.secondary">
                            --
                          </Typography>
                        </ListItem>
                      ))}
                    </List>
                  ) : (
                    <Typography variant="body2" color="text.secondary">Sem test cases recentes.</Typography>
                  )}
                </Paper>
              </Grid>

              <Grid item xs={12} lg={3}>
                <Stack spacing={1.7}>
                  <Paper elevation={0} sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                    <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
                      <Typography fontWeight={700}>{project?.name || 'Project'}</Typography>
                      <IconButton size="small"><EditOutlinedIcon fontSize="small" /></IconButton>
                    </Stack>
                    <Typography variant="body2" color="text.secondary">
                      This is your project's information section. You can edit this text to add a brief description and notes for the team.
                    </Typography>
                  </Paper>

                  <Paper elevation={0} sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                    <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
                      <Typography fontWeight={700}>Links</Typography>
                      <IconButton size="small"><EditOutlinedIcon fontSize="small" /></IconButton>
                    </Stack>
                    <Stack direction="row" spacing={1} alignItems="center" color="#4f46e5">
                      <LinkRoundedIcon fontSize="small" />
                      <Typography variant="body2">Attach your first link</Typography>
                    </Stack>
                  </Paper>

                  <Paper elevation={0} sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                    <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1.2 }}>
                      <Typography fontWeight={700}>Team</Typography>
                      <IconButton size="small"><EditOutlinedIcon fontSize="small" /></IconButton>
                    </Stack>

                    <Stack direction="row" spacing={1.1} alignItems="center" sx={{ mb: 1.6 }}>
                      <Avatar sx={{ width: 30, height: 30 }}>{displayName[0] || 'U'}</Avatar>
                      <Box>
                        <Typography fontWeight={600} fontSize={14}>{displayName}</Typography>
                        <Typography variant="caption" color="text.secondary">Project admin</Typography>
                      </Box>
                    </Stack>

                    <Paper
                      elevation={0}
                      sx={{
                        p: 1.5,
                        borderRadius: 1.5,
                        bgcolor: '#e9f6ef',
                        border: '1px solid #cce7d9',
                      }}
                    >
                      <Typography fontWeight={700} sx={{ mb: 0.6 }}>Invite Your Team</Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 1.1 }}>
                        Collaborate with your teammates, invite them to start using QA Sphere.
                      </Typography>
                      <Button variant="contained" size="small" startIcon={<PersonAddAlt1RoundedIcon />}>
                        Add Your Team
                      </Button>
                    </Paper>
                  </Paper>
                </Stack>
              </Grid>
            </Grid>
          </>
        )}
      </Container>
    </Box>
  );
}

function ProjectTestCasesPage() {
  const { projectId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const displayName = useMemo(() => toDisplayName(user?.username), [user?.username]);

  const [project, setProject] = useState(null);
  const [testCases, setTestCases] = useState([]);
  const [suites, setSuites] = useState([]);
  const [selectedFolder, setSelectedFolder] = useState('ROOT');
  const [selectedTestCaseId, setSelectedTestCaseId] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [activeCaseTab, setActiveCaseTab] = useState('overview');
  const [isCreateFolderOpen, setIsCreateFolderOpen] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeletingTestCase, setIsDeletingTestCase] = useState(false);

  const loadData = useCallback(async () => {
    if (!projectId) {
      return;
    }

    setIsLoading(true);
    setErrorMessage('');

    try {
      const [projectData, suiteTree, testCaseData] = await Promise.all([
        api.projects.getById(projectId),
        api.suites.tree(projectId),
        api.testCases.list(projectId),
      ]);

      setProject(projectData);
      setSuites(suiteTree || []);
      setTestCases(testCaseData || []);

      if ((testCaseData || []).length > 0) {
        setSelectedTestCaseId((current) => {
          if (current && (testCaseData || []).some((item) => item.id === current)) {
            return current;
          }

          return testCaseData[0].id;
        });
      } else {
        setSelectedTestCaseId('');
      }
    } catch (error) {
      setErrorMessage(error.message || 'Não foi possível carregar os test cases do projeto.');
      setProject(null);
      setSuites([]);
      setTestCases([]);
      setSelectedTestCaseId('');
    } finally {
      setIsLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const suiteNames = useMemo(() => {
    return (suites || []).map((suite) => suite.name).filter(Boolean);
  }, [suites]);

  const casesWithFolder = useMemo(() => {
    return (testCases || []).map((testCase) => {
      const inferred = inferFolderName(testCase.title);
      const matchedSuite = suiteNames.find((suiteName) =>
        testCase.title?.toLowerCase().includes(suiteName.toLowerCase()),
      );

      return {
        ...testCase,
        folderName: matchedSuite || inferred,
      };
    });
  }, [testCases, suiteNames]);

  const folders = useMemo(() => {
    const map = new Map();

    map.set('ALL', {
      key: 'ALL',
      label: 'All Folders',
      count: casesWithFolder.length,
      root: true,
    });

    map.set('ROOT', {
      key: 'ROOT',
      label: project?.name || 'Project',
      count: casesWithFolder.length,
      root: true,
    });

    suiteNames.forEach((suiteName) => {
      if (!map.has(suiteName)) {
        map.set(suiteName, { key: suiteName, label: suiteName, count: 0, root: false });
      }
    });

    casesWithFolder.forEach((item) => {
      const key = item.folderName;
      if (!map.has(key)) {
        map.set(key, { key, label: key, count: 0, root: false });
      }

      map.get(key).count += 1;
    });

    return Array.from(map.values());
  }, [casesWithFolder, project?.name, suiteNames]);

  const isFolderView = selectedFolder === 'ALL' || selectedFolder === 'ROOT';

  const visibleFolders = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();
    const onlyChildren = folders.filter((folder) => !folder.root);

    if (!query) {
      return onlyChildren;
    }

    return onlyChildren.filter((folder) => folder.label.toLowerCase().includes(query));
  }, [folders, searchTerm]);

  const filteredCases = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();

    return casesWithFolder.filter((item) => {
      const folderMatch = !isFolderView && item.folderName === selectedFolder;
      const queryMatch =
        !query ||
        (item.title || '').toLowerCase().includes(query) ||
        (item.testId || '').toLowerCase().includes(query);

      return folderMatch && queryMatch;
    });
  }, [casesWithFolder, selectedFolder, searchTerm, isFolderView]);

  useEffect(() => {
    if (isFolderView) {
      setSelectedTestCaseId('');
      return;
    }

    if (filteredCases.length === 0) {
      setSelectedTestCaseId('');
      return;
    }

    if (!filteredCases.some((item) => item.id === selectedTestCaseId)) {
      setSelectedTestCaseId(filteredCases[0].id);
    }
  }, [filteredCases, selectedTestCaseId, isFolderView]);

  const selectedCase = useMemo(() => {
    return filteredCases.find((item) => item.id === selectedTestCaseId) || null;
  }, [filteredCases, selectedTestCaseId]);

  const scenarioText = useMemo(() => {
    if (!selectedCase) {
      return '';
    }

    return [
      `@${(selectedCase.testId || 'case').toLowerCase()} ${(selectedCase.tagsKeywords || '').trim()}`.trim(),
      `Scenario: ${selectedCase.title}`,
      `  Given ${selectedCase.preconditions || 'o usuário está no contexto inicial'}`,
      `  When ${selectedCase.actions || 'executa os passos do teste'}`,
      `  Then ${selectedCase.expectedResult || 'o resultado esperado é atendido'}`,
    ].join('\n');
  }, [selectedCase]);

  const preconditionText = useMemo(() => {
    if (!selectedCase?.preconditions) {
      return 'Precondition not provided';
    }

    return selectedCase.preconditions
      .split(/\s*;\s*/)
      .map((item) => item.trim())
      .filter(Boolean)
      .join('\n');
  }, [selectedCase]);

  const handleCreateFolder = async () => {
    const name = newFolderName.trim();

    if (!name || !projectId) {
      return;
    }

    setIsSubmitting(true);

    try {
      await api.suites.create(projectId, name);
      setNewFolderName('');
      setIsCreateFolderOpen(false);
      await loadData();
      setSelectedFolder(name);
    } catch (error) {
      setErrorMessage(error.message || 'Não foi possível criar a pasta.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteSelectedTestCase = async () => {
    if (!projectId || !selectedCase?.id || isDeletingTestCase) {
      return;
    }

    const confirmed = window.confirm(`Deseja realmente excluir o test case "${selectedCase.title}"?`);
    if (!confirmed) {
      return;
    }

    setIsDeletingTestCase(true);
    setErrorMessage('');

    try {
      await api.testCases.remove(projectId, selectedCase.id);
      await loadData();
    } catch (error) {
      setErrorMessage(error.message || 'Não foi possível excluir o test case.');
    } finally {
      setIsDeletingTestCase(false);
    }
  };

  const openEditTestCasePage = () => {
    if (!selectedCase) {
      return;
    }
    navigate(`/projects/${projectId}/test-cases/${selectedCase.id}/edit`);
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#fff' }}>
      <TopBar displayName={displayName} projectName={project?.name || 'Project'} projectId={projectId} />

      <Container maxWidth={false} sx={{ px: 0, py: 0 }}>
        {errorMessage ? (
          <Alert severity="error" sx={{ m: 2 }}>
            {errorMessage}
          </Alert>
        ) : null}

        {isLoading ? (
          <Stack alignItems="center" py={8} spacing={1}>
            <CircularProgress size={24} />
            <Typography variant="body2" color="text.secondary">
              Carregando Test Cases...
            </Typography>
          </Stack>
        ) : (
          <>
            <Stack
              direction={{ xs: 'column', md: 'row' }}
              alignItems={{ xs: 'stretch', md: 'center' }}
              justifyContent="space-between"
              spacing={1.2}
              sx={{ px: 2.4, py: 2, borderBottom: '1px solid #e5e7eb' }}
            >
              <Box>
                <Typography variant="body2" color="text.secondary">
                  {project?.name || 'Project'} {isFolderView ? '' : '/'}
                </Typography>
                <Typography variant="h4" fontWeight={700} sx={{ fontSize: 34 }}>
                  {isFolderView ? project?.name || 'Project' : selectedFolder}
                </Typography>
              </Box>

              <Stack direction="row" spacing={1} alignItems="center">
                <TextField
                  size="small"
                  placeholder="Search"
                  value={searchTerm}
                  onChange={(event) => setSearchTerm(event.target.value)}
                  InputProps={{ startAdornment: <SearchRoundedIcon fontSize="small" /> }}
                  sx={{ width: 180 }}
                />
                <Button variant="outlined" onClick={() => setIsCreateFolderOpen(true)}>
                  Create Folder
                </Button>
                {!isFolderView && selectedCase ? (
                  <Button
                    variant="outlined"
                    color="error"
                    onClick={handleDeleteSelectedTestCase}
                    disabled={isDeletingTestCase}
                    sx={{
                      borderColor: '#dc2626',
                      color: '#dc2626',
                      '&:hover': {
                        borderColor: '#b91c1c',
                        bgcolor: 'rgba(220,38,38,0.06)',
                      },
                    }}
                  >
                    {isDeletingTestCase ? 'Deleting...' : '+ Delete'}
                  </Button>
                ) : null}
                <Button
                  variant="contained"
                  onClick={() => navigate(`/projects/${projectId}/test-cases/create`)}
                  sx={{ px: 2.2 }}
                >
                  + Create
                </Button>
                <IconButton sx={{ border: '1px solid', borderColor: 'divider' }}>
                  <MoreHorizRoundedIcon />
                </IconButton>
              </Stack>
            </Stack>

            <Grid container sx={{ minHeight: 'calc(100vh - 130px)' }}>
              <Grid item xs={12} md={2} sx={{ borderRight: '1px solid #e5e7eb', p: 1.5 }}>
                <Stack spacing={0.6}>
                  {folders.map((folder) => (
                    <Button
                      key={folder.key}
                      variant="text"
                      onClick={() => setSelectedFolder(folder.key)}
                      sx={{
                        justifyContent: 'flex-start',
                        color: selectedFolder === folder.key ? '#111827' : '#374151',
                        bgcolor: selectedFolder === folder.key ? '#f3f4f6' : 'transparent',
                        borderRadius: 1,
                        px: 1,
                        py: 0.7,
                      }}
                      startIcon={folder.key === 'ALL' ? <FolderOpenRoundedIcon fontSize="small" /> : <FolderOutlinedIcon fontSize="small" />}
                    >
                      <Stack direction="row" justifyContent="space-between" sx={{ width: '100%' }}>
                        <Typography variant="body2">{folder.label}</Typography>
                        <Typography variant="body2" color="text.secondary">{folder.count}</Typography>
                      </Stack>
                    </Button>
                  ))}
                </Stack>
              </Grid>

              <Grid item xs={12} md={isFolderView ? 10 : 5} sx={{ borderRight: '1px solid #e5e7eb', p: 2 }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1.5 }}>
                  <Button variant="outlined" size="small" startIcon={<FilterListRoundedIcon />}>
                    Filters
                  </Button>

                  <Typography variant="body2" color="text.secondary">
                    {isFolderView ? `${visibleFolders.length} folders` : `${filteredCases.length} test cases`}
                  </Typography>
                </Stack>

                {!isFolderView ? (
                  <Typography
                    variant="body2"
                    color="#4f46e5"
                    onClick={() => setSelectedFolder('ROOT')}
                    sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.6, mb: 1.2, cursor: 'pointer' }}
                  >
                    <ArrowOutwardRoundedIcon sx={{ fontSize: 14 }} />
                    Up to folder {project?.name || 'Project'}
                  </Typography>
                ) : null}

                <Paper elevation={0} sx={{ border: '1px solid', borderColor: '#e5e7eb', borderRadius: 1.5 }}>
                  {isFolderView ? (
                    visibleFolders.length > 0 ? (
                      visibleFolders.map((folder, index) => (
                        <Box
                          key={folder.key}
                          onClick={() => setSelectedFolder(folder.key)}
                          sx={{
                            px: 1.3,
                            py: 1.05,
                            borderTop: index === 0 ? 'none' : '1px solid #eef2f7',
                            bgcolor: '#fff',
                            cursor: 'pointer',
                            '&:hover': { bgcolor: '#f8fafc' },
                          }}
                        >
                          <Stack direction="row" alignItems="center" justifyContent="space-between">
                            <Stack direction="row" spacing={1} alignItems="center">
                              <FolderOutlinedIcon sx={{ fontSize: 18, color: '#6b7280' }} />
                              <Typography variant="body1">{folder.label}</Typography>
                            </Stack>
                            <Typography variant="caption" color="text.secondary">{folder.count}</Typography>
                          </Stack>
                        </Box>
                      ))
                    ) : (
                      <Box sx={{ p: 2 }}>
                        <Typography variant="body2" color="text.secondary">
                          Nenhuma pasta encontrada neste filtro.
                        </Typography>
                      </Box>
                    )
                  ) : filteredCases.length > 0 ? (
                    filteredCases.map((testCase, index) => (
                      <Box
                        key={testCase.id}
                        onClick={() => setSelectedTestCaseId(testCase.id)}
                        sx={{
                          px: 1.3,
                          py: 1.05,
                          borderTop: index === 0 ? 'none' : '1px solid #eef2f7',
                          bgcolor: selectedTestCaseId === testCase.id ? '#eef2ff' : '#fff',
                          cursor: 'pointer',
                          '&:hover': { bgcolor: '#f8fafc' },
                        }}
                      >
                        <Stack direction="row" alignItems="center" justifyContent="space-between">
                          <Typography variant="body1">{testCase.title}</Typography>
                          <Typography variant="caption" color="text.secondary">◇</Typography>
                        </Stack>
                      </Box>
                    ))
                  ) : (
                    <Box sx={{ p: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        Nenhum test case encontrado neste filtro.
                      </Typography>
                    </Box>
                  )}
                </Paper>

                <Button
                  variant="text"
                  sx={{ mt: 1.2, color: '#4f46e5' }}
                  onClick={() => navigate(`/projects/${projectId}/test-cases/create`)}
                >
                  + Quick test case creation
                </Button>
              </Grid>

              {!isFolderView ? (
              <Grid
                item
                xs={12}
                md={5}
                sx={{
                  p: 2.2,
                  maxHeight: 'calc(100vh - 130px)',
                  overflowY: 'auto',
                }}
              >
                {selectedCase ? (
                  <>
                    <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
                      <Stack direction="row" spacing={0.8} alignItems="center">
                        <Typography variant="body2" color="text.secondary">{selectedCase.testId || 'AMA-000'}</Typography>
                        <Box sx={{ px: 0.8, py: 0.2, bgcolor: '#eef2f7', borderRadius: 1 }}>
                          <Typography variant="caption">@{selectedCase.folderName?.toLowerCase() || 'login'}</Typography>
                        </Box>
                      </Stack>
                      <Stack direction="row" spacing={0.6}>
                        <IconButton size="small"><MoreHorizRoundedIcon fontSize="small" /></IconButton>
                        <IconButton size="small" onClick={openEditTestCasePage}><EditOutlinedIcon fontSize="small" /></IconButton>
                      </Stack>
                    </Stack>

                    <Typography variant="h4" fontWeight={700} sx={{ fontSize: 42, lineHeight: 1.1, mb: 1.1 }}>
                      {selectedCase.title}
                    </Typography>

                    <Tabs
                      value={activeCaseTab}
                      onChange={(_, value) => setActiveCaseTab(value)}
                      textColor="primary"
                      indicatorColor="primary"
                      sx={{ mb: 2 }}
                    >
                      <Tab value="overview" label="Overview" sx={{ textTransform: 'none' }} />
                      <Tab value="insights" label="Insights" sx={{ textTransform: 'none' }} />
                    </Tabs>

                    <Typography variant="h6" fontWeight={700} sx={{ mb: 1 }}>
                      Precondition
                    </Typography>

                    <Paper
                      elevation={0}
                      sx={{
                        p: 1.2,
                        borderRadius: 1.3,
                        bgcolor: '#1f243a',
                        color: '#fff',
                        whiteSpace: 'pre-wrap',
                        wordBreak: 'break-word',
                        fontFamily: 'Consolas, monospace',
                        fontSize: 13,
                        mb: 2,
                      }}
                    >
                      {preconditionText}
                    </Paper>

                    <Typography variant="h6" fontWeight={700} sx={{ mb: 1 }}>
                      Test Step
                    </Typography>

                    <Paper
                      elevation={0}
                      sx={{
                        p: 1.2,
                        borderRadius: 1.3,
                        bgcolor: '#1f243a',
                        color: '#fff',
                        whiteSpace: 'pre-wrap',
                        wordBreak: 'break-word',
                        fontFamily: 'Consolas, monospace',
                        fontSize: 13,
                        mb: 2,
                      }}
                    >
                      {scenarioText}
                    </Paper>

                    <Typography variant="h6" fontWeight={700} sx={{ mb: 0.9 }}>
                      Expected Result
                    </Typography>
                    <Stack component="ul" sx={{ pl: 2.2, mt: 0, mb: 2.2 }} spacing={0.5}>
                      {splitExpectedResult(selectedCase.expectedResult).map((item) => (
                        <Typography component="li" key={item} variant="body2" color="text.secondary">
                          {item}
                        </Typography>
                      ))}
                    </Stack>

                    <Divider sx={{ mb: 1.6 }} />

                    <Typography variant="h6" fontWeight={700} sx={{ mb: 1 }}>
                      Edit History
                    </Typography>

                    <Stack spacing={1}>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Avatar sx={{ width: 26, height: 26 }}>{displayName[0] || 'U'}</Avatar>
                        <Typography variant="body2" color="text.secondary">
                          {displayName} updated steps · 4 days ago
                        </Typography>
                      </Stack>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Avatar sx={{ width: 26, height: 26 }}>{displayName[0] || 'U'}</Avatar>
                        <Typography variant="body2" color="text.secondary">
                          {displayName} created test case · 4 days ago
                        </Typography>
                      </Stack>
                    </Stack>
                  </>
                ) : (
                  <Stack alignItems="center" justifyContent="center" sx={{ height: '100%' }} spacing={1.1}>
                    <PersonOutlineRoundedIcon color="disabled" />
                    <Typography variant="body2" color="text.secondary" textAlign="center">
                      Selecione um test case para visualizar detalhes.
                    </Typography>
                  </Stack>
                )}
              </Grid>
              ) : null}
            </Grid>
          </>
        )}
      </Container>

      <Dialog open={isCreateFolderOpen} onClose={() => setIsCreateFolderOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Create Folder</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            autoFocus
            label="Folder name"
            value={newFolderName}
            onChange={(event) => setNewFolderName(event.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsCreateFolderOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateFolder} disabled={isSubmitting}>
            {isSubmitting ? 'Creating...' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

    </Box>
  );
}

function RichTextStepField({ label, value, onChange, placeholder }) {
  const wrapperRef = useRef(null);
  const editorRef = useRef(null);
  const [isActive, setIsActive] = useState(false);

  useEffect(() => {
    if (!editorRef.current) {
      return;
    }

    const normalizedEditorValue = toEditorHtml(value || '');
    if (document.activeElement !== editorRef.current && editorRef.current.innerHTML !== normalizedEditorValue) {
      editorRef.current.innerHTML = normalizedEditorValue;
    }
  }, [value]);

  const emitValue = () => {
    onChange(editorRef.current?.innerHTML || '');
  };

  const executeCommand = (command, commandValue) => {
    editorRef.current?.focus();
    document.execCommand(command, false, commandValue);
    emitValue();
  };

  const commandButtons = [
    { icon: <FormatBoldRoundedIcon fontSize="small" />, action: () => executeCommand('bold') },
    { icon: <FormatItalicRoundedIcon fontSize="small" />, action: () => executeCommand('italic') },
    { icon: <FormatUnderlinedRoundedIcon fontSize="small" />, action: () => executeCommand('underline') },
    { icon: <StrikethroughSRoundedIcon fontSize="small" />, action: () => executeCommand('strikeThrough') },
    {
      icon: <FormatQuoteRoundedIcon fontSize="small" />,
      action: () => executeCommand('formatBlock', '<blockquote>'),
    },
    { icon: <FormatListBulletedRoundedIcon fontSize="small" />, action: () => executeCommand('insertUnorderedList') },
    { icon: <FormatListNumberedRoundedIcon fontSize="small" />, action: () => executeCommand('insertOrderedList') },
    {
      icon: <InsertLinkRoundedIcon fontSize="small" />,
      action: () => {
        const href = window.prompt('Informe a URL do link:');
        if (href) {
          executeCommand('createLink', href);
        }
      },
    },
    {
      icon: <SentimentSatisfiedAltRoundedIcon fontSize="small" />,
      action: () => executeCommand('insertText', '😊 '),
    },
    {
      icon: <ImageRoundedIcon fontSize="small" />,
      action: () => {
        const imageUrl = window.prompt('Informe a URL da imagem:');
        if (imageUrl) {
          executeCommand('insertImage', imageUrl);
        }
      },
    },
    {
      icon: <VideocamRoundedIcon fontSize="small" />,
      action: () => {
        const videoUrl = window.prompt('Informe a URL do vídeo:');
        if (videoUrl) {
          executeCommand('insertHTML', `<a href="${videoUrl}" target="_blank" rel="noopener noreferrer">Vídeo</a>`);
        }
      },
    },
    {
      icon: <CodeRoundedIcon fontSize="small" />,
      action: () => executeCommand('insertHTML', '<code>trecho de código</code>'),
    },
    {
      icon: <TableChartRoundedIcon fontSize="small" />,
      action: () =>
        executeCommand(
          'insertHTML',
          '<table border="1" style="border-collapse:collapse;width:100%"><tr><th>Coluna 1</th><th>Coluna 2</th></tr><tr><td>Valor</td><td>Valor</td></tr></table>',
        ),
    },
  ];

  const handleBlur = () => {
    window.setTimeout(() => {
      if (!wrapperRef.current?.contains(document.activeElement)) {
        setIsActive(false);
      }
    }, 0);
  };

  return (
    <Box ref={wrapperRef}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 0.7 }}>
        <Typography variant="body2" fontWeight={600}>
          {label}
        </Typography>
        <IconButton
          size="small"
          sx={{
            border: '1px solid',
            borderColor: 'divider',
            width: 28,
            height: 28,
            visibility: isActive ? 'visible' : 'hidden',
          }}
        >
          <MoreHorizRoundedIcon sx={{ fontSize: 16 }} />
        </IconButton>
      </Stack>

      <Paper
        elevation={0}
        variant="outlined"
        sx={{
          borderRadius: 1.5,
          borderColor: isActive ? '#4f46e5' : '#e5e7eb',
          transition: 'border-color 0.2s ease',
          overflow: 'hidden',
        }}
        onClick={() => {
          setIsActive(true);
          editorRef.current?.focus();
        }}
      >
        <Stack
          direction="row"
          spacing={0.2}
          sx={{
            px: 0.8,
            py: 0.5,
            minHeight: 40,
            borderBottom: '1px solid',
            borderColor: '#e5e7eb',
            visibility: 'visible',
            opacity: 1,
            pointerEvents: 'auto',
          }}
        >
          {commandButtons.map((button, index) => (
            <IconButton
              key={`${label}-toolbar-${index}`}
              size="small"
              onMouseDown={(event) => {
                event.preventDefault();
                button.action();
              }}
            >
              {button.icon}
            </IconButton>
          ))}
        </Stack>

        <Box
          ref={editorRef}
          contentEditable
          suppressContentEditableWarning
          data-placeholder={placeholder}
          onFocus={() => setIsActive(true)}
          onBlur={handleBlur}
          onInput={emitValue}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault();
              executeCommand('insertLineBreak');
              return;
            }

            if (event.key === 'Tab') {
              event.preventDefault();

              if (event.shiftKey) {
                executeCommand('outdent');
              } else {
                executeCommand('indent');
              }
            }
          }}
          sx={{
            minHeight: 40,
            px: 1.4,
            py: 1,
            fontSize: 15,
            lineHeight: 1.45,
            outline: 'none',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            '&:empty:before': {
              content: 'attr(data-placeholder)',
              color: '#9ca3af',
            },
            '& blockquote': {
              borderLeft: '3px solid #c7d2fe',
              margin: '4px 0',
              paddingLeft: 8,
              color: '#374151',
            },
            '& code': {
              bgcolor: '#eef2ff',
              borderRadius: 1,
              px: 0.4,
              py: 0.1,
              fontFamily: 'Consolas, monospace',
            },
            '& table': {
              width: '100%',
              borderCollapse: 'collapse',
            },
            '& th, & td': {
              border: '1px solid #e5e7eb',
              padding: '4px 6px',
              fontSize: 13,
            },
          }}
        />
      </Paper>
    </Box>
  );
}

function ProjectTestCaseCreatePage() {
  const { projectId, testCaseId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isEditMode = Boolean(testCaseId);

  const displayName = useMemo(() => toDisplayName(user?.username), [user?.username]);

  const [project, setProject] = useState(null);
  const [requirements, setRequirements] = useState([]);
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedAttachment, setSelectedAttachment] = useState(null);
  const attachmentInputRef = useRef(null);
  const [form, setForm] = useState({
    title: '',
    testId: '',
    priority: 'Medium',
    bugSeverity: 'Major',
    tagsKeywords: '',
    requirementLink: '',
    executionType: 'Manual',
    testCaseStatus: 'Draft',
    platform: '',
    testEnvironment: '',
    preconditions: '',
    actions: '',
    expectedResult: '',
    executionStatus: 'Not Run',
    notes: '',
    attachments: '',
  });

  useEffect(() => {
    let isCancelled = false;

    const loadCreateContext = async () => {
      if (!projectId) {
        return;
      }

      setIsLoading(true);
      setErrorMessage('');

      try {
        const [projectData, requirementData, testCaseData] = await Promise.all([
          api.projects.getById(projectId),
          api.requirements.list(projectId),
          isEditMode && testCaseId ? api.testCases.getById(projectId, testCaseId) : Promise.resolve(null),
        ]);

        if (isCancelled) {
          return;
        }

        setProject(projectData);
        setRequirements(requirementData || []);

        if (testCaseData) {
          setForm({
            title: testCaseData.title || '',
            testId: testCaseData.testId || '',
            priority: testCaseData.priority || 'Medium',
            bugSeverity: testCaseData.bugSeverity || 'Major',
            tagsKeywords: testCaseData.tagsKeywords || '',
            requirementLink: testCaseData.requirementLink || '',
            executionType: testCaseData.executionType || 'Manual',
            testCaseStatus: normalizeTestCaseStatusValue(testCaseData.testCaseStatus),
            platform: testCaseData.platform || '',
            testEnvironment: testCaseData.testEnvironment || '',
            preconditions: testCaseData.preconditions || '',
            actions: testCaseData.actions || '',
            expectedResult: testCaseData.expectedResult || '',
            executionStatus: testCaseData.executionStatus || 'Not Run',
            notes: testCaseData.notes || '',
            attachments: testCaseData.attachments || '',
          });
        }
      } catch (error) {
        if (!isCancelled) {
          setErrorMessage(error.message || 'Não foi possível carregar contexto do formulário de test case.');
          setProject(null);
          setRequirements([]);
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    loadCreateContext();

    return () => {
      isCancelled = true;
    };
  }, [projectId, testCaseId, isEditMode]);

  const updateField = (key) => (event) => {
    setForm((current) => ({
      ...current,
      [key]: event.target.value,
    }));
  };

  const updateRichField = (key) => (nextValue) => {
    setForm((current) => ({
      ...current,
      [key]: nextValue,
    }));
  };

  const clearForNext = () => {
    setForm((current) => ({
      ...current,
      title: '',
      testId: '',
      priority: 'Medium',
      bugSeverity: 'Major',
      tagsKeywords: '',
      requirementLink: '',
      executionType: 'Manual',
      testCaseStatus: 'Draft',
      platform: '',
      testEnvironment: '',
      preconditions: '',
      actions: '',
      expectedResult: '',
      executionStatus: 'Not Run',
      notes: '',
      attachments: '',
    }));

    setSelectedAttachment(null);
    if (attachmentInputRef.current) {
      attachmentInputRef.current.value = '';
    }
  };

  const handlePickAttachment = () => {
    attachmentInputRef.current?.click();
  };

  const handleAttachmentChange = (event) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    if (!isAllowedAttachmentFile(file)) {
      setErrorMessage('Somente imagens, Excel (.xls/.xlsx) ou CSV são permitidos.');
      setSelectedAttachment(null);
      event.target.value = '';
      return;
    }

    if (file.size > MAX_ATTACHMENT_SIZE_BYTES) {
      setErrorMessage('O arquivo deve ter no máximo 1MB.');
      setSelectedAttachment(null);
      event.target.value = '';
      return;
    }

    setErrorMessage('');
    setSelectedAttachment(file);
  };

  const submitCreate = async (mode) => {
    if (!projectId) {
      return;
    }

    if (!form.title.trim()) {
      setErrorMessage('Título do test case é obrigatório.');
      return;
    }

    if (selectedAttachment) {
      if (!isAllowedAttachmentFile(selectedAttachment)) {
        setErrorMessage('Somente imagens, Excel (.xls/.xlsx) ou CSV são permitidos.');
        return;
      }

      if (selectedAttachment.size > MAX_ATTACHMENT_SIZE_BYTES) {
        setErrorMessage('O arquivo deve ter no máximo 1MB.');
        return;
      }
    }

    setIsSubmitting(true);
    setErrorMessage('');

    const payload = {
      title: form.title.trim(),
      testId: form.testId.trim() || undefined,
      priority: form.priority,
      bugSeverity: form.bugSeverity,
      tagsKeywords: form.tagsKeywords.trim() || undefined,
      requirementLink: form.requirementLink.trim() || undefined,
      executionType: form.executionType,
      testCaseStatus: isEditMode
        ? normalizeTestCaseStatusValue(form.testCaseStatus)
        : mode === 'draft'
          ? 'Draft'
          : 'Ready for Review',
      platform: form.platform.trim() || undefined,
      testEnvironment: form.testEnvironment.trim() || undefined,
      preconditions: richTextToPlain(form.preconditions) || undefined,
      actions: richTextToPlain(form.actions) || undefined,
      expectedResult: richTextToPlain(form.expectedResult) || undefined,
      executionStatus: form.executionStatus,
      notes: form.notes.trim() || undefined,
      attachments: form.attachments.trim() || undefined,
    };

    try {
      if (isEditMode && testCaseId) {
        await api.testCases.update(projectId, testCaseId, payload);

        if (selectedAttachment) {
          await api.testCases.uploadAttachment(projectId, testCaseId, selectedAttachment);
        }

        navigate(`/projects/${projectId}/test-cases`);
      } else {
        const created = await api.testCases.create(projectId, payload);

        if (selectedAttachment && created?.id) {
          await api.testCases.uploadAttachment(projectId, created.id, selectedAttachment);
        }

        if (mode === 'create-and-another') {
          clearForNext();
        } else {
          navigate(`/projects/${projectId}/test-cases`);
        }
      }
    } catch (error) {
      setErrorMessage(error.message || `Não foi possível ${isEditMode ? 'editar' : 'criar'} o test case.`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const injectAiDraft = () => {
    setForm((current) => ({
      ...current,
      preconditions:
        current.preconditions ||
        'o usuário está autenticado e posicionado na página de login',
      actions:
        current.actions ||
        'informa credenciais válidas e confirma o envio do formulário',
      expectedResult:
        current.expectedResult ||
        'deve autenticar com sucesso e redirecionar para o dashboard principal',
    }));
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#fff' }}>
      <TopBar displayName={displayName} projectName={project?.name || 'Project'} projectId={projectId} />

      <Container maxWidth={false} sx={{ px: { xs: 2, md: 2.2 }, py: 2.2, pb: 12 }}>
        {errorMessage ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {errorMessage}
          </Alert>
        ) : null}

        {isLoading ? (
          <Stack alignItems="center" py={10} spacing={1}>
            <CircularProgress size={24} />
            <Typography variant="body2" color="text.secondary">
              Preparando formulário de criação...
            </Typography>
          </Stack>
        ) : (
          <>
            <Typography variant="h4" fontWeight={700} sx={{ mb: 2.3, fontSize: 40 }}>
              {isEditMode ? 'Edit Test Case' : 'Create Test Case'}
            </Typography>

            <Grid container spacing={2.4} justifyContent="flex-start" sx={{ alignItems: 'flex-start' }}>
              <Grid
                item
                xs={12}
                md={6}
                sx={{
                  width: { xs: '100%', md: '50%' },
                  maxWidth: { xs: '100%', md: '50%' },
                  flexGrow: 0,
                  flexShrink: 0,
                }}
              >
                <Stack spacing={1.4}>
                  <Box>
                    <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                      Title *
                    </Typography>
                    <TextField
                      fullWidth
                      value={form.title}
                      onChange={updateField('title')}
                      placeholder="Test case title"
                      size="small"
                    />
                  </Box>

                  <Box>
                    <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                      Test ID
                    </Typography>
                    <TextField
                      fullWidth
                      value={form.testId}
                      onChange={updateField('testId')}
                      placeholder="AMQA-001"
                      size="small"
                    />
                  </Box>

                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.2}>
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                        Priority
                      </Typography>
                      <TextField
                        fullWidth
                        select
                        size="small"
                        value={form.priority}
                        onChange={updateField('priority')}
                      >
                        <MenuItem value="Low">Low</MenuItem>
                        <MenuItem value="Medium">Medium</MenuItem>
                        <MenuItem value="High">High</MenuItem>
                        <MenuItem value="Critical">Critical</MenuItem>
                      </TextField>
                    </Box>

                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                        Tags
                      </Typography>
                      <TextField
                        fullWidth
                        size="small"
                        value={form.tagsKeywords}
                        onChange={updateField('tagsKeywords')}
                        placeholder="Select tags"
                      />
                    </Box>
                  </Stack>

                  <Box>
                    <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                      Requirement
                    </Typography>
                    <TextField
                      fullWidth
                      select
                      size="small"
                      value={form.requirementLink}
                      onChange={updateField('requirementLink')}
                    >
                      <MenuItem value="">Select or create new</MenuItem>
                      {requirements.map((requirement) => (
                        <MenuItem key={requirement.id} value={requirement.id}>
                          {requirement.title}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Box>

                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.2}>
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                        Bug Severity
                      </Typography>
                      <TextField fullWidth select size="small" value={form.bugSeverity} onChange={updateField('bugSeverity')}>
                        <MenuItem value="Blocker">Blocker</MenuItem>
                        <MenuItem value="Critical">Critical</MenuItem>
                        <MenuItem value="Major">Major</MenuItem>
                        <MenuItem value="Minor">Minor</MenuItem>
                        <MenuItem value="Trivial">Trivial</MenuItem>
                      </TextField>
                    </Box>

                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                        Execution Type
                      </Typography>
                      <TextField fullWidth select size="small" value={form.executionType} onChange={updateField('executionType')}>
                        <MenuItem value="Manual">Manual</MenuItem>
                        <MenuItem value="Automated">Automated</MenuItem>
                      </TextField>
                    </Box>
                  </Stack>

                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.2}>
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                        Test Case Status
                      </Typography>
                      <TextField fullWidth select size="small" value={form.testCaseStatus} onChange={updateField('testCaseStatus')}>
                        <MenuItem value="Draft">Draft</MenuItem>
                        <MenuItem value="Ready for Review">Ready for Review</MenuItem>
                        <MenuItem value="Review in Progress">Review in Progress</MenuItem>
                        <MenuItem value="Rework">Rework</MenuItem>
                        <MenuItem value="Final">Final</MenuItem>
                        <MenuItem value="Future">Future</MenuItem>
                        <MenuItem value="Obsolete">Obsolete</MenuItem>
                      </TextField>
                    </Box>

                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                        Execution Status
                      </Typography>
                      <TextField fullWidth select size="small" value={form.executionStatus} onChange={updateField('executionStatus')}>
                        <MenuItem value="Not Run">Not Run</MenuItem>
                        <MenuItem value="Passed">Passed</MenuItem>
                        <MenuItem value="Failed">Failed</MenuItem>
                        <MenuItem value="Blocked">Blocked</MenuItem>
                      </TextField>
                    </Box>
                  </Stack>

                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.2}>
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                        Platform
                      </Typography>
                      <TextField fullWidth size="small" value={form.platform} onChange={updateField('platform')} placeholder="Web" />
                    </Box>

                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body2" fontWeight={600} sx={{ mb: 0.7 }}>
                        Test Environment
                      </Typography>
                      <TextField
                        fullWidth
                        size="small"
                        value={form.testEnvironment}
                        onChange={updateField('testEnvironment')}
                        placeholder="Staging"
                      />
                    </Box>
                  </Stack>

                  <Stack direction="row" spacing={2} sx={{ py: 0.5 }}>
                    <Button
                      variant="text"
                      startIcon={<AttachFileRoundedIcon />}
                      sx={{ color: '#4f46e5' }}
                      onClick={handlePickAttachment}
                    >
                      Attach file
                    </Button>
                    <Button variant="text" startIcon={<LinkRoundedIcon />} sx={{ color: '#4f46e5' }}>
                      Attach link
                    </Button>
                  </Stack>

                  <input
                    ref={attachmentInputRef}
                    type="file"
                    hidden
                    accept="image/*,.xlsx,.xls,.csv"
                    onChange={handleAttachmentChange}
                  />

                  {selectedAttachment ? (
                    <Typography variant="caption" color="text.secondary">
                      Arquivo selecionado: {selectedAttachment.name} ({Math.ceil(selectedAttachment.size / 1024)} KB)
                    </Typography>
                  ) : null}

                  <Divider />

                  <RichTextStepField
                    label="Precondition"
                    value={form.preconditions}
                    onChange={updateRichField('preconditions')}
                    placeholder="Descreva as pré-condições do cenário..."
                  />

                  <RichTextStepField
                    label="Action"
                    value={form.actions}
                    onChange={updateRichField('actions')}
                    placeholder="Descreva a ação principal do teste..."
                  />

                  <RichTextStepField
                    label="Expected Result"
                    value={form.expectedResult}
                    onChange={updateRichField('expectedResult')}
                    placeholder="Descreva o resultado esperado..."
                  />

                  <TextField
                    fullWidth
                    multiline
                    minRows={2}
                    label="Notes"
                    value={form.notes}
                    onChange={updateField('notes')}
                    size="small"
                  />

                  <TextField
                    fullWidth
                    label="Attachments (URL/list)"
                    value={form.attachments}
                    onChange={updateField('attachments')}
                    size="small"
                  />

                  <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mt: 0.4 }}>
                    <IconButton sx={{ border: '1px solid', borderColor: 'divider', width: 34, height: 34 }}>
                      +
                    </IconButton>

                    <Button variant="outlined" sx={{ borderRadius: 999 }}>
                      Shared Steps
                    </Button>
                  </Stack>
                </Stack>
              </Grid>

              <Grid
                item
                xs={12}
                md={6}
                sx={{
                  width: { xs: '100%', md: '50%' },
                  maxWidth: { xs: '100%', md: '50%' },
                  flexGrow: 0,
                  flexShrink: 0,
                }}
              >
                <Paper
                  elevation={0}
                  sx={{
                    border: '1px solid',
                    borderColor: '#e5e7eb',
                    borderRadius: 2,
                    p: 2,
                    maxWidth: 420,
                  }}
                >
                  <Stack spacing={1}>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <AutoAwesomeRoundedIcon sx={{ color: '#6b5cff', fontSize: 20 }} />
                      <Typography fontWeight={700}>Write with AI</Typography>
                    </Stack>
                    <Typography variant="body2" color="text.secondary">
                      Create a detailed test case with our AI-powered assistant. Using your project details and your input,
                      our AI generates an initial description that you can easily customize.
                    </Typography>
                    <Button variant="contained" onClick={injectAiDraft} sx={{ alignSelf: 'flex-start', mt: 0.6 }}>
                      Start writing
                    </Button>
                  </Stack>
                </Paper>
              </Grid>
            </Grid>
          </>
        )}
      </Container>

      <Box
        sx={{
          position: 'fixed',
          left: 0,
          right: 0,
          bottom: 0,
          borderTop: '1px solid',
          borderColor: '#e5e7eb',
          bgcolor: '#fff',
          px: 1,
          py: 1,
          zIndex: 10,
        }}
      >
        <Stack direction="row" spacing={1.2} alignItems="center">
          <Button variant="outlined" onClick={() => navigate(`/projects/${projectId}/test-cases`)}>
            Cancel
          </Button>
          {isEditMode ? (
            <Button variant="contained" onClick={() => submitCreate('edit')} disabled={isSubmitting}>
              {isSubmitting ? 'Saving...' : 'Save Changes'}
            </Button>
          ) : (
            <>
              <Button variant="outlined" onClick={() => submitCreate('create-and-another')} disabled={isSubmitting}>
                Create and Add Another
              </Button>
              <Button variant="outlined" onClick={() => submitCreate('draft')} disabled={isSubmitting}>
                Save as Draft
              </Button>
              <Button variant="contained" onClick={() => submitCreate('create')} disabled={isSubmitting}>
                {isSubmitting ? 'Creating...' : 'Create'}
              </Button>
            </>
          )}
        </Stack>
      </Box>
    </Box>
  );
}

function ProjectRedirect() {
  const { projectId } = useParams();

  if (!projectId) {
    return <Navigate to="/" replace />;
  }

  return <Navigate to={`/projects/${projectId}/overview`} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <HomePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects/:projectId"
        element={
          <ProtectedRoute>
            <ProjectRedirect />
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects/:projectId/overview"
        element={
          <ProtectedRoute>
            <ProjectOverviewPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects/:projectId/test-cases"
        element={
          <ProtectedRoute>
            <ProjectTestCasesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects/:projectId/test-cases/create"
        element={
          <ProtectedRoute>
            <ProjectTestCaseCreatePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects/:projectId/test-cases/:testCaseId/edit"
        element={
          <ProtectedRoute>
            <ProjectTestCaseCreatePage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}