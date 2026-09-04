package fp

// case class — незмінний запис даних (поля val, без setter-ів).
// у Java наближено: final class + private final поля + equals/hashCode вручну.
final case class Donation(donor: String, amount: Double)
