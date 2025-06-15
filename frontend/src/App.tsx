import { useState, useEffect } from 'react';
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
  Alert
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
      <Container maxWidth="sm">
        <Box sx={{ mt: 4, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <Typography variant="h4" component="h1" gutterBottom>
            Вход в систему
          </Typography>
          <TextField
            label="ID пользователя"
            value={inputUserId}
            onChange={(e) => setInputUserId(e.target.value)}
            fullWidth
            margin="normal"
          />
          <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
            <Button variant="contained" onClick={handleLogin}>
              Войти
            </Button>
            <Button variant="outlined" onClick={handleCreateAccount}>
              Создать аккаунт
            </Button>
          </Box>
        </Box>
        <Snackbar open={!!notification} autoHideDuration={6000} onClose={handleCloseNotification}>
          <Alert onClose={handleCloseNotification} severity={notification?.severity}>
            {notification?.message}
          </Alert>
        </Snackbar>
      </Container>
    );
  }

  return (
    <Container maxWidth="md">
      <Box sx={{ my: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Система заказов
        </Typography>
        
        <Paper sx={{ p: 2, mb: 2, bgcolor: 'primary.main', color: 'primary.contrastText' }}>
          <Typography variant="h6" gutterBottom>
            ID пользователя: {userId}
          </Typography>
          <Typography variant="h6">
            Баланс: {balance} ₽
          </Typography>
        </Paper>

        <Box sx={{ mb: 2 }}>
          <Button 
            variant="contained" 
            color="primary" 
            onClick={() => setIsOrderDialogOpen(true)}
            sx={{ mr: 2 }}
          >
            Создать заказ
          </Button>
          <Button 
            variant="contained" 
            color="secondary" 
            onClick={() => setIsDepositDialogOpen(true)}
          >
            Пополнить баланс
          </Button>
        </Box>

        <Typography variant="h5" gutterBottom>
          Мои заказы
        </Typography>
        {orders.length === 0 ? (
          <Typography>У вас пока нет заказов</Typography>
        ) : (
          <List>
            {orders.map((order) => (
              <Paper key={order.id} sx={{ mb: 2, p: 2 }}>
                <ListItem>
                  <ListItemText
                    primary={`Заказ #${order.id}`}
                    secondary={
                      <>
                        <Typography component="span" variant="body2">
                          Сумма: {order.amount} ₽
                        </Typography>
                        <br />
                        <Typography component="span" variant="body2">
                          Статус: {order.status}
                        </Typography>
                        <br />
                        <Typography component="span" variant="body2">
                          Создан: {new Date(order.createdAt).toLocaleString()}
                        </Typography>
                      </>
                    }
                  />
                </ListItem>
              </Paper>
            ))}
          </List>
        )}
      </Box>

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
            inputProps={{ min: "0", step: "0.01" }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsOrderDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleCreateOrder} variant="contained">
            Создать
          </Button>
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
            inputProps={{ min: "0", step: "0.01" }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsDepositDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleDeposit} variant="contained">
            Пополнить
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!notification} autoHideDuration={6000} onClose={handleCloseNotification}>
        <Alert onClose={handleCloseNotification} severity={notification?.severity}>
          {notification?.message}
        </Alert>
      </Snackbar>
    </Container>
  );
}

export default App; 