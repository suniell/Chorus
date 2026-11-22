package com.sunil.chorus.data

// The stories that come with the app.
// Pride and Prejudice (1813) is public domain. The other three are written for Chorus.
object SampleStories {

    private val pride = Story(
        id = "pride",
        title = "Pride and Prejudice",
        author = "Jane Austen",
        coverStyle = 4,
        characters = listOf(
            Character(NARRATOR, "Narrator", voice = 5),
            Character("mrs_bennet", "Mrs. Bennet", voice = 0, pitch = 1.25f, rate = 1.1f),
            Character("mr_bennet", "Mr. Bennet", voice = 1, pitch = 0.8f, rate = 0.9f),
        ),
        lines = listOf(
            Line(NARRATOR, "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife."),
            Line("mrs_bennet", "My dear Mr. Bennet, have you heard that Netherfield Park is let at last?"),
            Line(NARRATOR, "Mr. Bennet replied that he had not."),
            Line("mrs_bennet", "But it is, for Mrs. Long has just been here, and she told me all about it."),
            Line(NARRATOR, "Mr. Bennet made no answer."),
            Line("mrs_bennet", "Do you not want to know who has taken it?"),
            Line("mr_bennet", "You want to tell me, and I have no objection to hearing it."),
            Line(NARRATOR, "This was invitation enough."),
        ),
    )

    private val lighthouse = Story(
        id = "lighthouse",
        title = "The Lighthouse Keeper",
        author = "Chorus Originals",
        coverStyle = 0,
        characters = listOf(
            Character(NARRATOR, "Narrator", voice = 5),
            Character("maya", "Maya", voice = 1, pitch = 1.15f),
            Character("theo", "Theo", voice = 2, pitch = 0.95f, rate = 1.15f),
            Character("keeper", "The Keeper", voice = 3, pitch = 0.7f, rate = 0.85f),
        ),
        lines = listOf(
            Line(NARRATOR, "The lighthouse at Gull Point had been dark for eleven years."),
            Line("maya", "Then who just turned on the light?"),
            Line("theo", "Nobody. That's the problem."),
            Line(NARRATOR, "Somewhere above them, a door creaked open, and slow footsteps started down the iron stairs."),
            Line("maya", "Theo. Theo, someone's coming."),
            Line("theo", "I'll wait in the car. I'll wait in the car forever, actually."),
            Line("keeper", "You're late. I've kept the light burning for you both."),
            Line(NARRATOR, "Maya grabbed Theo's sleeve. Neither of them had told anyone they were coming."),
        ),
    )

    private val station = Story(
        id = "station",
        title = "Moonlit Station",
        author = "Chorus Originals",
        coverStyle = 3,
        characters = listOf(
            Character(NARRATOR, "Narrator", voice = 5),
            Character("iris", "Iris", voice = 3, pitch = 1.1f),
            Character("sam", "Sam", voice = 4, pitch = 0.85f),
        ),
        lines = listOf(
            Line(NARRATOR, "The last train out of Ashford was forty minutes late, and the platform was empty except for two strangers and one umbrella."),
            Line("iris", "I think that umbrella is big enough for two, if you don't mind sharing."),
            Line("sam", "I never mind sharing. I just mind trains that promise nine fifteen and mean ten."),
            Line("iris", "Then we have something in common already."),
            Line(NARRATOR, "Far down the tracks, a single light appeared, and neither of them moved toward it."),
            Line("sam", "We could wait for the next one."),
            Line("iris", "There isn't a next one."),
            Line("sam", "I know."),
        ),
    )

    private val crowns = Story(
        id = "crowns",
        title = "Paper Crowns",
        author = "Chorus Originals",
        coverStyle = 1,
        characters = listOf(
            Character(NARRATOR, "Narrator", voice = 5),
            Character("queen", "The Paper Queen", voice = 0, pitch = 1.15f, rate = 0.95f),
            Character("fox", "Sir Fox", voice = 4, pitch = 0.9f, rate = 1.15f),
            Character("pip", "Pip", voice = 1, pitch = 1.4f, rate = 1.2f),
        ),
        lines = listOf(
            Line(NARRATOR, "In the kingdom under the kitchen table, the paper queen called her council to order."),
            Line("queen", "Who has stolen the royal biscuit?"),
            Line("fox", "Your Majesty, I was asleep in the shoe box all afternoon. Ask anyone."),
            Line("pip", "He wasn't! I saw crumbs on his tail!"),
            Line("fox", "Those are decorative crumbs."),
            Line("queen", "Decorative crumbs are still crumbs, Sir Fox."),
            Line(NARRATOR, "And so the council voted, eleven to one, to search the shoe box."),
        ),
    )

    val all = listOf(pride, lighthouse, station, crowns)
}