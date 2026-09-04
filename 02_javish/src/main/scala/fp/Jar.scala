package fp

final case class Jar(
    id: String,
    goal: Double,
    // у Java: private final List<Donation> donations — список лише для читання.
    donations: List[Donation]
)
