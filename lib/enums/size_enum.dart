enum Size{
  medium(0), //normal size text
  bold(1), //only bold text
  boldMedium(2), //bold with medium
  boldLarge(3), //bold with large
  extraLarge(4) //extra large

  const Size(this.value);

  final int value;
}