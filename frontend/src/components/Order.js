import React from 'react';
import OrderStatusTracker from './OrderStatusTracker';
import { ToastContainer } from 'react-toastify';

const Order = ({ order }) => {
    return (
        <div className="order-card">
            <OrderStatusTracker orderId={order.id} />
            <ToastContainer />
            <h3>Заказ #{order.id}</h3>
            <p>Статус: {order.status}</p>
            <p>Сумма: {order.amount}</p>
            <p>Дата создания: {new Date(order.createdAt).toLocaleString()}</p>
        </div>
    );
};

export default Order; 