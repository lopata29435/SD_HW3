import React, { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

const OrderStatusTracker = ({ orderId }) => {
    const [stompClient, setStompClient] = useState(null);
    const [connected, setConnected] = useState(false);

    useEffect(() => {
        // Создаем клиент STOMP
        const client = new Client({
            brokerURL: 'ws://localhost:8080/ws',
            onConnect: () => {
                console.log('Подключено к WebSocket');
                setConnected(true);
                
                // Подписываемся на обновления статуса заказа
                client.subscribe(`/topic/orders/${orderId}`, (message) => {
                    const notification = JSON.parse(message.body);
                    console.log('Получено обновление статуса:', notification);
                    
                    // Показываем уведомление
                    toast.info(notification.message, {
                        position: "top-right",
                        autoClose: 5000,
                        hideProgressBar: false,
                        closeOnClick: true,
                        pauseOnHover: true,
                        draggable: true,
                    });
                });
            },
            onDisconnect: () => {
                console.log('Отключено от WebSocket');
                setConnected(false);
            },
            onError: (error) => {
                console.error('Ошибка WebSocket:', error);
                toast.error('Ошибка подключения к серверу');
            }
        });

        // Активируем клиент
        client.activate();
        setStompClient(client);

        // Очистка при размонтировании
        return () => {
            if (client) {
                client.deactivate();
            }
        };
    }, [orderId]);

    return null; // Компонент не рендерит ничего видимого
};

export default OrderStatusTracker; 