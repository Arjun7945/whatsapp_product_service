import 'package:equatable/equatable.dart';

class CustomerOrder extends Equatable {
  final int? id;
  final int customerId;
  final String customerName;
  final double totalAmount;
  final String status;
  final int? deliveryPersonId;
  final List<OrderItem>? items;

  const CustomerOrder({
    this.id,
    required this.customerId,
    required this.customerName,
    required this.totalAmount,
    required this.status,
    this.deliveryPersonId,
    this.items,
  });

  factory CustomerOrder.fromJson(Map<String, dynamic> json) {
    return CustomerOrder(
      id: json['id'] as int?,
      customerId: json['customerId'] as int,
      customerName: json['customerName'] as String,
      totalAmount: (json['totalAmount'] as num).toDouble(),
      status: json['status'] as String,
      deliveryPersonId: json['deliveryPersonId'] as int?,
      items: json['items'] != null
          ? (json['items'] as List)
              .map((item) => OrderItem.fromJson(item))
              .toList()
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      'customerId': customerId,
      'customerName': customerName,
      'totalAmount': totalAmount,
      'status': status,
      if (deliveryPersonId != null) 'deliveryPersonId': deliveryPersonId,
      if (items != null) 'items': items!.map((item) => item.toJson()).toList(),
    };
  }

  @override
  List<Object?> get props => [
        id,
        customerId,
        customerName,
        totalAmount,
        status,
        deliveryPersonId,
        items,
      ];
}

class OrderItem extends Equatable {
  final int? id;
  final int fishProductId;
  final String fishProductName;
  final double quantity;
  final double pricePerKg;
  final double totalPrice;

  const OrderItem({
    this.id,
    required this.fishProductId,
    required this.fishProductName,
    required this.quantity,
    required this.pricePerKg,
    required this.totalPrice,
  });

  factory OrderItem.fromJson(Map<String, dynamic> json) {
    return OrderItem(
      id: json['id'] as int?,
      fishProductId: json['fishProductId'] as int,
      fishProductName: json['fishProductName'] as String,
      quantity: (json['quantity'] as num).toDouble(),
      pricePerKg: (json['pricePerKg'] as num).toDouble(),
      totalPrice: (json['totalPrice'] as num).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      'fishProductId': fishProductId,
      'fishProductName': fishProductName,
      'quantity': quantity,
      'pricePerKg': pricePerKg,
      'totalPrice': totalPrice,
    };
  }

  @override
  List<Object?> get props => [
        id,
        fishProductId,
        fishProductName,
        quantity,
        pricePerKg,
        totalPrice,
      ];
}
