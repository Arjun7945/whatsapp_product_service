import 'package:equatable/equatable.dart';

class FishProduct extends Equatable {
  final int? id;
  final String name;
  final double pricePerKg;
  final String? imageURL;
  final bool isAvailable;

  const FishProduct({
    this.id,
    required this.name,
    required this.pricePerKg,
    this.imageURL,
    required this.isAvailable,
  });

  factory FishProduct.fromJson(Map<String, dynamic> json) {
    return FishProduct(
      id: json['id'] as int?,
      name: json['name'] as String,
      pricePerKg: (json['pricePerKg'] as num).toDouble(),
      imageURL: json['imageURL'] as String?,
      isAvailable: json['isAvailable'] as bool? ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      'name': name,
      'pricePerKg': pricePerKg,
      if (imageURL != null) 'imageURL': imageURL,
      'isAvailable': isAvailable,
    };
  }

  FishProduct copyWith({
    int? id,
    String? name,
    double? pricePerKg,
    String? imageURL,
    bool? isAvailable,
  }) {
    return FishProduct(
      id: id ?? this.id,
      name: name ?? this.name,
      pricePerKg: pricePerKg ?? this.pricePerKg,
      imageURL: imageURL ?? this.imageURL,
      isAvailable: isAvailable ?? this.isAvailable,
    );
  }

  @override
  List<Object?> get props => [id, name, pricePerKg, imageURL, isAvailable];
}
