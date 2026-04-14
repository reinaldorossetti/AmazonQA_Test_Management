import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  IconButton,
  InputAdornment,
  Link,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import { Link as RouterLink, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

const quickUsers = [
  { label: 'Admin', email: 'admin@amazonqa.local' },
  { label: 'Leader', email: 'leader@amazonqa.local' },
  { label: 'Tester', email: 'tester@amazonqa.local' },
  { label: 'Guest', email: 'guest@amazonqa.local' },
];

export function LoginPage() {
  const { login, isLoading, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('admin@amazonqa.local');
  const [password, setPassword] = useState('strong-pass');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  if (isLoggedIn) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage('');

    try {
      await login(email, password);
      const redirectTo = location.state?.from || '/';
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setErrorMessage(error.message || 'Não foi possível autenticar.');
    }
  };

  const applyQuickUser = (userEmail) => {
    setEmail(userEmail);
    setPassword('strong-pass');
  };

  return (
    <Box className="login-shell">
      <Box className="login-blob login-blob--one" aria-hidden />
      <Box className="login-blob login-blob--two" aria-hidden />
      <Box className="login-blob login-blob--three" aria-hidden />

      <Paper className="login-card" elevation={0} component="section">
        <Stack spacing={2.5}>
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Box className="login-icon">
              <LockOutlinedIcon />
            </Box>
            <Box>
              <Typography variant="h5" fontWeight={700}>
                Entrar na plataforma
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Amazon QA Test Management • backend Kotlin integrado
              </Typography>
            </Box>
          </Stack>

          <Box>
            <Stack direction="row" spacing={1} mb={1.2} flexWrap="wrap" useFlexGap>
              {quickUsers.map((user) => (
                <Chip
                  key={user.label}
                  label={user.label}
                  size="small"
                  onClick={() => applyQuickUser(user.email)}
                  clickable
                />
              ))}
            </Stack>
            <Typography variant="caption" color="text.secondary" display="flex" alignItems="center" gap={0.75}>
              <AutoAwesomeIcon sx={{ fontSize: 14 }} />
              Dica: clique em um perfil acima para preencher rápido.
            </Typography>
          </Box>

          {errorMessage ? <Alert severity="error">{errorMessage}</Alert> : null}

          <Box component="form" onSubmit={handleSubmit} noValidate>
            <Stack spacing={2}>
              <TextField
                label="E-mail"
                type="email"
                fullWidth
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                autoComplete="email"
                autoFocus
              />

              <TextField
                label="Senha"
                type={showPassword ? 'text' : 'password'}
                fullWidth
                required
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        edge="end"
                        onClick={() => setShowPassword((current) => !current)}
                        aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
                      >
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />

              <Button type="submit" variant="contained" size="large" disabled={isLoading}>
                {isLoading ? (
                  <Stack direction="row" spacing={1} alignItems="center">
                    <CircularProgress size={18} color="inherit" />
                    <span>Entrando...</span>
                  </Stack>
                ) : (
                  'Entrar'
                )}
              </Button>

              <Typography variant="caption" color="text.secondary" textAlign="center">
                Endpoint utilizado: <strong>POST /api/v1/auth/login</strong>
              </Typography>

              <Typography variant="caption" color="text.secondary" textAlign="center">
                Ainda não tem usuário?{' '}
                <Link component={RouterLink} to="/register" underline="hover">
                  Criar conta
                </Link>
              </Typography>
            </Stack>
          </Box>
        </Stack>
      </Paper>
    </Box>
  );
}