import React, { useState, useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import axios from 'axios';
import { 
  Container, 
  Typography, 
  Box, 
  Button, 
  TextField, 
  List, 
  ListItem, 
  ListItemText, 
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
  Alert,
  Grid
} from '@mui/material';

interface Order {
  id: string;
  userId: string;
  amount: number;
  status: string;
  createdAt: string;
}

const API_BASE_URL = 'http://localhost:8080';

function App() {
  const [userId, setUserId] = useState<string>('');
  const [orders, setOrders] = useState<Order[]>([]);
  const [balance, setBalance] = useState<number>(0);
  const [inputUserId, setInputUserId] = useState('');
  const [orderAmount, setOrderAmount] = useState<string>('');
  const [depositAmount, setDepositAmount] = useState<string>('');
  const [isOrderDialogOpen, setIsOrderDialogOpen] = useState(false);
  const [isDepositDialogOpen, setIsDepositDialogOpen] = useState(false);
  const [notification, setNotification] = useState<{ message: string; severity: 'success' | 'error' } | null>(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    if (userId) {
      const client = new Client({
        brokerURL: 'ws://localhost:8080/ws',
        connectHeaders: {
          'X-User-Id': userId
        },
        debug: function (str) {
          console.log('STOMP: ' + str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        webSocketFactory: () => {
          return new WebSocket('ws://localhost:8080/ws');
        }
      });

      client.onConnect = () => {
        console.log('Connected to WebSocket');
        client.subscribe('/topic/orders', (message) => {
          const update = JSON.parse(message.body);
          console.log('Получено обновление статуса:', update);
          setOrders(prevOrders => 
            prevOrders.map(order => 
              order.id === update.orderId 
                ? { ...order, status: update.status }
                : order
            )
          );
          setNotification({
            message: `Статус заказа ${update.orderId} изменен на ${update.status}`,
            severity: 'success'
          });
        });
      };

      client.onStompError = (frame) => {
        console.error('STOMP error:', frame);
      };

      client.onWebSocketError = (event) => {
        console.error('WebSocket error:', event);
      };

      client.activate();
      return () => {
        client.deactivate();
      };
    }
  }, [userId]);

  const fetchData = async () => {
    if (!userId) return;
    
    try {
      const [ordersResponse, balanceResponse] = await Promise.all([
        axios.get(`${API_BASE_URL}/order/api/orders`, {
          headers: { 'X-User-Id': userId }
        }),
        axios.get(`${API_BASE_URL}/payment/api/accounts/${userId}/balance`, {
          headers: { 'X-User-Id': userId }
        })
      ]);
      setOrders(ordersResponse.data);
      setBalance(balanceResponse.data);
    } catch (error) {
      console.error('Error fetching data:', error);
      setNotification({
        message: 'Ошибка при загрузке данных',
        severity: 'error'
      });
    }
  };

  useEffect(() => {
    console.log('isLoggedIn changed:', isLoggedIn);
    console.log('userId:', userId);
    if (isLoggedIn) {
      fetchData();
    }
  }, [isLoggedIn, userId]);

  const handleLogin = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/payment/api/accounts/check/${inputUserId}`);
      console.log('Login response:', response.data);
      
      if (response.data === true) {
        console.log('Setting userId to:', inputUserId);
        setUserId(inputUserId);
        console.log('Setting isLoggedIn to true');
        setIsLoggedIn(true);
        setNotification({
          message: 'Успешный вход',
          severity: 'success'
        });
      } else {
        setNotification({
          message: 'Аккаунт не найден',
          severity: 'error'
        });
      }
    } catch (error) {
      console.error('Error checking account:', error);
      setNotification({
        message: 'Ошибка при проверке аккаунта',
        severity: 'error'
      });
    }
  };

  const handleCreateAccount = async () => {
    try {
      await axios.post(`${API_BASE_URL}/payment/api/accounts`, null, {
        headers: { 'X-User-Id': inputUserId }
      });
      setUserId(inputUserId);
      setIsLoggedIn(true);
      setNotification({
        message: 'Аккаунт успешно создан',
        severity: 'success'
      });
    } catch (error) {
      console.error('Error creating account:', error);
      setNotification({
        message: 'Ошибка при создании аккаунта',
        severity: 'error'
      });
    }
  };

  const handleCreateOrder = async () => {
    try {
      const amount = parseFloat(orderAmount);
      if (isNaN(amount) || amount <= 0) {
        setNotification({
          message: 'Пожалуйста, введите корректную сумму',
          severity: 'error'
        });
        return;
      }

      await axios.post(`${API_BASE_URL}/order/api/orders`, null, {
        headers: { 'X-User-Id': userId },
        params: { amount: amount.toFixed(2) }
      });
      setIsOrderDialogOpen(false);
      setOrderAmount('');
      fetchData();
      setNotification({
        message: 'Заказ успешно создан',
        severity: 'success'
      });
    } catch (error) {
      console.error('Error creating order:', error);
      setNotification({
        message: 'Ошибка при создании заказа',
        severity: 'error'
      });
    }
  };

  const handleDeposit = async () => {
    try {
      const amount = parseFloat(depositAmount);
      if (isNaN(amount) || amount <= 0) {
        setNotification({
          message: 'Пожалуйста, введите корректную сумму',
          severity: 'error'
        });
        return;
      }

      await axios.post(`${API_BASE_URL}/payment/api/accounts/${userId}/deposit`, null, {
        params: { amount }
      });
      setIsDepositDialogOpen(false);
      setDepositAmount('');
      fetchData();
      setNotification({
        message: 'Баланс успешно пополнен',
        severity: 'success'
      });
    } catch (error) {
      console.error('Error depositing:', error);
      setNotification({
        message: 'Ошибка при пополнении баланса',
        severity: 'error'
      });
    }
  };

  const handleCloseNotification = () => {
    setNotification(null);
  };

  if (!isLoggedIn) {
    return (
      <Box sx={{ 
        minHeight: '100vh',
        backgroundImage: 'url("/background.png")',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
        position: 'relative'
      }}>
        <Box sx={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          zIndex: 1
        }} />
        <Container maxWidth="lg" sx={{ position: 'relative', zIndex: 2, py: 4 }}>
          <Paper elevation={3} sx={{ p: 4, maxWidth: 400, mx: 'auto', mt: 8, backgroundColor: 'rgba(255, 255, 255, 0.9)' }}>
            <Typography variant="h4" component="h1" gutterBottom align="center" sx={{ color: '#1976d2' }}>
              Вход в систему
            </Typography>
            <TextField
              fullWidth
              label="ID пользователя"
              value={inputUserId}
              onChange={(e) => setInputUserId(e.target.value)}
              margin="normal"
            />
            <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
              <Button
                fullWidth
                variant="contained"
                onClick={handleLogin}
                sx={{ 
                  backgroundColor: '#1976d2',
                  '&:hover': {
                    backgroundColor: '#1565c0'
                  }
                }}
              >
                Войти
              </Button>
              <Button
                fullWidth
                variant="outlined"
                onClick={handleCreateAccount}
                sx={{ 
                  borderColor: '#1976d2',
                  color: '#1976d2',
                  '&:hover': {
                    borderColor: '#1565c0',
                    backgroundColor: 'rgba(25, 118, 210, 0.04)'
                  }
                }}
              >
                Создать аккаунт
              </Button>
            </Box>
          </Paper>

          {notification && (
            <Snackbar
              open={!!notification}
              autoHideDuration={6000}
              onClose={() => setNotification(null)}
              anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
            >
              <Alert onClose={() => setNotification(null)} severity={notification.severity}>
                {notification.message}
              </Alert>
            </Snackbar>
          )}
        </Container>
      </Box>
    );
  }

  return (
    <Box sx={{ 
      minHeight: '100vh',
      backgroundImage: 'url("/background.png")',
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
      position: 'relative'
    }}>
      <Box sx={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        zIndex: 1
      }} />
      <Container maxWidth="lg" sx={{ position: 'relative', zIndex: 2, py: 4 }}>
        <Paper elevation={3} sx={{ p: 3, mb: 3, backgroundColor: 'rgba(255, 255, 255, 0.9)' }}>
          <Typography variant="h4" component="h1" gutterBottom align="center" sx={{ color: '#1976d2' }}>
            Личный кабинет
          </Typography>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" sx={{ color: '#1976d2' }}>
              ID пользователя: {userId}
            </Typography>
            <Typography variant="h6" sx={{ color: '#1976d2' }}>
              Баланс: {balance} ₽
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
            <Button
              variant="contained"
              onClick={() => setIsOrderDialogOpen(true)}
              sx={{ 
                backgroundColor: '#1976d2',
                '&:hover': {
                  backgroundColor: '#1565c0'
                }
              }}
            >
              Создать заказ
            </Button>
            <Button
              variant="outlined"
              onClick={() => setIsDepositDialogOpen(true)}
              sx={{ 
                borderColor: '#1976d2',
                color: '#1976d2',
                '&:hover': {
                  borderColor: '#1565c0',
                  backgroundColor: 'rgba(25, 118, 210, 0.04)'
                }
              }}
            >
              Пополнить баланс
            </Button>
          </Box>
        </Paper>

        <Paper elevation={3} sx={{ p: 3, backgroundColor: 'rgba(255, 255, 255, 0.9)' }}>
          <Typography variant="h5" component="h2" gutterBottom sx={{ color: '#1976d2' }}>
            Мои заказы
          </Typography>
          <Grid container spacing={3}>
            {orders.map((order) => (
              <Grid item xs={12} sm={6} md={4} key={order.id}>
                <Paper elevation={2} sx={{ p: 2, height: '100%', backgroundColor: 'rgba(255, 255, 255, 0.95)' }}>
                  <Typography variant="h6" gutterBottom sx={{ color: '#1976d2' }}>
                    Заказ #{order.id}
                  </Typography>
                  <Typography>Статус: {order.status}</Typography>
                  <Typography>Сумма: {order.amount} ₽</Typography>
                  <Typography>Дата: {new Date(order.createdAt).toLocaleString()}</Typography>
                </Paper>
              </Grid>
            ))}
          </Grid>
        </Paper>

        <Dialog open={isOrderDialogOpen} onClose={() => setIsOrderDialogOpen(false)}>
          <DialogTitle>Создать заказ</DialogTitle>
          <DialogContent>
            <TextField
              autoFocus
              margin="dense"
              label="Сумма заказа"
              type="number"
              fullWidth
              value={orderAmount}
              onChange={(e) => setOrderAmount(e.target.value)}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setIsOrderDialogOpen(false)}>Отмена</Button>
            <Button onClick={handleCreateOrder} variant="contained">Создать</Button>
          </DialogActions>
        </Dialog>

        <Dialog open={isDepositDialogOpen} onClose={() => setIsDepositDialogOpen(false)}>
          <DialogTitle>Пополнить баланс</DialogTitle>
          <DialogContent>
            <TextField
              autoFocus
              margin="dense"
              label="Сумма пополнения"
              type="number"
              fullWidth
              value={depositAmount}
              onChange={(e) => setDepositAmount(e.target.value)}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setIsDepositDialogOpen(false)}>Отмена</Button>
            <Button onClick={handleDeposit} variant="contained">Пополнить</Button>
          </DialogActions>
        </Dialog>

        {notification && (
          <Snackbar
            open={!!notification}
            autoHideDuration={6000}
            onClose={() => setNotification(null)}
            anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
          >
            <Alert onClose={() => setNotification(null)} severity={notification.severity}>
              {notification.message}
            </Alert>
          </Snackbar>
        )}
      </Container>
    </Box>
  );
}

export default App; 