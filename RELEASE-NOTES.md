Release Notes: semantic-id
==========================

# 3.0.3

* Use dashes instead of dots (breaks backward compatibility)
  * Why? - Dots are dropped because of (1) confusion with filename extensions and (2) resemblance to wildcards
* PadlessBase32 use lowercase for output

# 2.0.2

* SemanticIdCodec now retains casing for given prefix (backward compatible)
* PadlessBase32 uses uppercase characters for output
* Add IdCodec.encodeRandomBytes method for generating random IDs

# 2.0.1

* Add encode and decode methods for byte array and UUID keys
* Add canDecode method
* Refactor factory methods of SemanticIdCodec class

# 1.0.0

Initial version
