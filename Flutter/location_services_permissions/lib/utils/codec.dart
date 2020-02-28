class Codec {
  static Map<String, dynamic> encodeLocationServicesPermissions(
          bool showDenyForeverDialog,
          bool showRationalDialog,
          String rationaleText) =>
      <String, dynamic>{
        'showDenyForeverDialog': showDenyForeverDialog,
        'showRationalDialog': showRationalDialog,
        'rationaleText': rationaleText
      };

  static T encodeEnum<T>(Iterable<T> values, dynamic value) {
    return values.firstWhere((type) => type.toString().split(".").last == value,
        orElse: () => null);
  }
}
