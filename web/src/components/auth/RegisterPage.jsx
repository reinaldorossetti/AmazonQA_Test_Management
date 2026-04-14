import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Link,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import PersonAddAlt1OutlinedIcon from '@mui/icons-material/PersonAddAlt1Outlined';
import { Link as RouterLink, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

const INITIAL_FORM = {
  person_type: 'PF',
  first_name: '',
  last_name: '',
  email: '',
  phone: '',
  password: '',
  confirm_password: '',
  address_city: '',
  address_state: '',
};

export function RegisterPage() {
  const { register, isLoading, isLoggedIn } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState(INITIAL_FORM);
  const [errorMessage, setErrorMessage] = useState('');

  if (isLoggedIn) {
    return <Navigate to="/" replace />;
  }

  const setField = (field) => (event) => {
    setForm((prev) => ({
      ...prev,
      [field]: event.target.value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage('');

    if (form.password !== form.confirm_password) {
      setErrorMessage('As senhas não conferem.');
      return;
    }

    if (!form.email || !form.password) {
      setErrorMessage('E-mail e senha são obrigatórios.');
      return;
    }

    try {
      const payload = {
        person_type: form.person_type,
        first_name: form.first_name,
        last_name: form.last_name,
        email: form.email,
        phone: form.phone,
        password: form.password,
        address_city: form.address_city,
        address_state: form.address_state,
      };

      await register(payload);
      navigate('/', { replace: true });
    } catch (error) {
      setErrorMessage(error.message || 'Não foi possível concluir o cadastro.');
    }
  };

  return (
    <Box className="login-shell">
      <Box className="login-blob login-blob--one" aria-hidden />
      <Box className="login-blob login-blob--two" aria-hidden />
      <Box className="login-blob login-blob--three" aria-hidden />

      <Paper className="login-card auth-card--wide" elevation={0} component="section">
        <Stack spacing={2.2}>
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Box className="login-icon">
              <PersonAddAlt1OutlinedIcon />
            </Box>
            <Box>
              <Typography variant="h5" fontWeight={700}>
                Criar conta
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Cadastro público integrado em <strong>POST /api/v1/users/register</strong>
              </Typography>
            </Box>
          </Stack>

          {errorMessage ? <Alert severity="error">{errorMessage}</Alert> : null}

          <Box component="form" onSubmit={handleSubmit} noValidate>
            <Stack spacing={1.6}>
              <TextField
                select
                label="Tipo de pessoa"
                value={form.person_type}
                onChange={setField('person_type')}
              >
                <MenuItem value="PF">Pessoa Física (PF)</MenuItem>
                <MenuItem value="PJ">Pessoa Jurídica (PJ)</MenuItem>
              </TextField>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.4}>
                <TextField label="Nome" value={form.first_name} onChange={setField('first_name')} fullWidth />
                <TextField label="Sobrenome" value={form.last_name} onChange={setField('last_name')} fullWidth />
              </Stack>

              <TextField
                label="E-mail"
                type="email"
                value={form.email}
                onChange={setField('email')}
                autoComplete="email"
                required
                fullWidth
              />

              <TextField
                label="Telefone"
                value={form.phone}
                onChange={setField('phone')}
                autoComplete="tel"
                fullWidth
              />

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.4}>
                <TextField
                  label="Cidade"
                  value={form.address_city}
                  onChange={setField('address_city')}
                  fullWidth
                />
                <TextField
                  label="Estado"
                  value={form.address_state}
                  onChange={setField('address_state')}
                  fullWidth
                />
              </Stack>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.4}>
                <TextField
                  label="Senha"
                  type="password"
                  value={form.password}
                  onChange={setField('password')}
                  autoComplete="new-password"
                  required
                  fullWidth
                />
                <TextField
                  label="Confirmar senha"
                  type="password"
                  value={form.confirm_password}
                  onChange={setField('confirm_password')}
                  autoComplete="new-password"
                  required
                  fullWidth
                />
              </Stack>

              <Button type="submit" variant="contained" size="large" disabled={isLoading}>
                {isLoading ? (
                  <Stack direction="row" spacing={1} alignItems="center">
                    <CircularProgress size={18} color="inherit" />
                    <span>Criando conta...</span>
                  </Stack>
                ) : (
                  'Criar conta e entrar'
                )}
              </Button>

              <Typography variant="caption" color="text.secondary" textAlign="center">
                Já possui conta?{' '}
                <Link component={RouterLink} to="/login" underline="hover">
                  Entrar agora
                </Link>
              </Typography>
            </Stack>
          </Box>
        </Stack>
      </Paper>
    </Box>
  );
}