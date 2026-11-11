package ng.unicycle.data

/**
 * PROTOTYPE CONTENT. Bundled so the app is usable before a backend exists.
 * The UI only talks to [ContentRepository]; swap this for a repository backed by the
 * institution directory / course catalogue API. Nothing here is verified academic material:
 * every question is labelled "Under review" with the source "UniCycle prototype sample".
 */
object PrototypeContent : ContentRepository {
    override val isPrototype = true

    const val APT_NUM = "APT-NUM"

    private val institutions = listOf(
        Institution("unilag", "University of Lagos", InstitutionType.Federal),
        Institution("ui", "University of Ibadan", InstitutionType.Federal),
        Institution("oau", "Obafemi Awolowo University", InstitutionType.Federal),
        Institution("abu", "Ahmadu Bello University", InstitutionType.Federal),
        Institution("lasu", "Lagos State University", InstitutionType.State),
        Institution("oou", "Olabisi Onabanjo University", InstitutionType.State),
        Institution("covenant", "Covenant University", InstitutionType.Private),
        Institution("babcock", "Babcock University", InstitutionType.Private),
        Institution("yabatech", "Yaba College of Technology", InstitutionType.Polytechnic),
        Institution("fcoe-akoka", "Federal College of Education (Technical), Akoka", InstitutionType.CollegeOfEducation),
    )

    private val faculties = listOf(
        Faculty("sci", "Science"), Faculty("eng", "Engineering"), Faculty("soc", "Social Sciences"),
        Faculty("art", "Arts"), Faculty("hlt", "Health Sciences"),
    )

    private val departments = listOf(
        Department("csc", "sci", "Computer Science"), Department("bio", "sci", "Biological Sciences"),
        Department("mth", "sci", "Mathematics"),
        Department("eee", "eng", "Electrical Engineering"), Department("mee", "eng", "Mechanical Engineering"),
        Department("eco", "soc", "Economics"), Department("pol", "soc", "Political Science"),
        Department("eng-lit", "art", "English"), Department("his", "art", "History"),
        Department("nur", "hlt", "Nursing"), Department("phm", "hlt", "Pharmacy"),
    )

    private fun degreeFor(d: Department) = when {
        d.id == "phm" -> "B.Pharm."
        d.facultyId == "eng" -> "B.Eng."
        d.facultyId == "art" -> "B.A."
        else -> "B.Sc."
    }

    private val courses = listOf(
        Course("GST 111", "Communication in English", "Grammar, comprehension and essay writing for university study.", 100, emptySet(), listOf("Grammar", "Comprehension", "Essay writing")),
        Course("MTH 101", "Elementary Mathematics I (Calculus)", "Limits, differentiation and integration of single-variable functions.", 100, setOf("csc", "mth", "eee", "mee", "bio", "eco"), listOf("Limits", "Differentiation", "Integration")),
        Course("PHY 101", "General Physics I", "Mechanics, waves and thermal physics.", 100, setOf("csc", "mth", "eee", "mee", "bio"), listOf("Mechanics", "Waves")),
        Course("BIO 101", "General Biology I", "Cell structure, genetics and transport across membranes.", 100, setOf("bio", "nur", "phm"), listOf("Cell Biology", "Genetics", "Transport")),
        Course("CSC 201", "Data Structures", "Core data structures and the analysis of algorithms.", 200, setOf("csc", "eee"), listOf("Stacks and queues", "Complexity", "Trees")),
        Course("ECO 201", "Principles of Microeconomics", "Demand, supply, opportunity cost and market structures.", 200, setOf("eco", "pol"), listOf("Demand", "Opportunity cost", "Market structures")),
    )

    private const val SRC = "UniCycle prototype sample"

    private fun q(
        id: String, code: String, topic: String, year: Int?, diff: Difficulty, text: String,
        options: List<String>, correct: Int, eli10: String, simple: String, university: String, wrong: String,
    ) = PastQuestion(id, code, topic, year, SRC, Provenance.UnderReview, diff, text, options, correct, eli10, simple, university, wrong)

    private val questions = listOf(
        q("mth101-1", "MTH 101", "Differentiation", 2022, Difficulty.Easy,
            "What is the derivative of f(x) = x³ with respect to x?",
            listOf("3x²", "x²", "3x", "x⁴/4"), 0,
            "Think of the derivative as a slope-finder. Bring the little number on top (3) down in front, then make that little number one smaller. So x³ becomes 3x².",
            "The power rule says the derivative of xⁿ is n·xⁿ⁻¹. With n = 3 you get 3x².",
            "By the power rule, d/dx[xⁿ] = n·xⁿ⁻¹. For n = 3 this gives 3x², the slope of the tangent to y = x³ at each x.",
            "x² forgets to multiply by the power. 3x lowers the power too far. x⁴/4 is the integral, not the derivative."),
        q("mth101-2", "MTH 101", "Limits", 2021, Difficulty.Medium,
            "What is the value of lim (x→0) sin(x)/x ?",
            listOf("0", "1", "∞", "It does not exist"), 1,
            "When x is a tiny angle, sin(x) is almost exactly the same size as x. So sin(x) divided by x is almost exactly 1.",
            "This is a standard limit. As x gets very close to 0 (in radians), sin(x)/x gets closer and closer to 1.",
            "From cos x ≤ sin x / x ≤ 1 near 0 and the squeeze theorem, lim sin x / x = 1. It is the basis of the derivative of sin x.",
            "0 would need sin(x) to shrink much faster than x, which it does not. The limit is finite and exists, so ∞ and 'does not exist' are wrong."),
        q("mth101-3", "MTH 101", "Integration", 2022, Difficulty.Easy,
            "Evaluate ∫ 2x dx.",
            listOf("x² + C", "2x² + C", "x + C", "2 + C"), 0,
            "Integrating undoes finding a slope. Which expression has slope 2x? That is x². We add C because constants disappear when you find a slope.",
            "Raise the power by one and divide by the new power: 2·x²/2 = x², plus a constant C.",
            "∫ 2x dx = 2·(x²/2) + C = x² + C by the power rule for integration, where C is the arbitrary constant of integration.",
            "2x² + C forgets to divide by the new power. x + C and 2 + C lower the power instead of raising it."),

        q("csc201-1", "CSC 201", "Stacks and queues", 2022, Difficulty.Easy,
            "Which data structure follows the Last-In, First-Out (LIFO) principle?",
            listOf("Stack", "Queue", "Binary tree", "Hash table"), 0,
            "Picture a pile of plates. You add a plate on top and take from the top, so the last plate you added is the first you use.",
            "A stack adds (push) and removes (pop) items at the same end, so the newest item leaves first.",
            "A stack supports push and pop in O(1) at one end, giving LIFO order. It underlies function call frames and expression evaluation.",
            "A queue is First-In, First-Out. A binary tree and a hash table do not enforce a LIFO add/remove order."),
        q("csc201-2", "CSC 201", "Complexity", 2021, Difficulty.Medium,
            "What is the worst-case time complexity of binary search on a sorted array of n elements?",
            listOf("O(1)", "O(log n)", "O(n)", "O(n log n)"), 1,
            "Guess a number from 1 to 1000 by always asking 'higher or lower?' and cutting the choices in half. You need only about 10 guesses.",
            "Each step halves the search range, so the number of steps grows like log₂ n.",
            "Binary search halves the remaining interval per comparison. The recurrence T(n) = T(n/2) + O(1) solves to O(log n).",
            "O(1) would mean one step whatever the size. O(n) is linear search. O(n log n) is typical of sorting, not searching."),
        q("csc201-3", "CSC 201", "Trees", 2022, Difficulty.Medium,
            "An in-order traversal of a binary search tree visits the keys in what order?",
            listOf("Ascending order", "Descending order", "Level by level", "The order they were inserted"), 0,
            "In a search tree, smaller numbers sit on the left and bigger ones on the right. Visiting left, then the node, then right lists everything from smallest to biggest.",
            "In-order means left subtree, node, then right subtree. For a binary search tree that gives sorted (ascending) output.",
            "By the BST invariant (left < node < right), in-order traversal yields keys in non-decreasing order, which is why it is used to sort or validate a BST.",
            "Descending order needs right-node-left. Level order visits by depth using a queue. Insertion order is not preserved by the tree shape."),

        q("bio101-1", "BIO 101", "Cell Biology", 2022, Difficulty.Easy,
            "Which organelle is the main site of ATP production in a eukaryotic cell?",
            listOf("Mitochondrion", "Ribosome", "Golgi apparatus", "Lysosome"), 0,
            "Mitochondria are the cell's tiny power stations. They turn food into energy the cell can use.",
            "Mitochondria carry out aerobic respiration, which produces most of the cell's ATP.",
            "Oxidative phosphorylation on the inner mitochondrial membrane generates most cellular ATP in eukaryotes.",
            "Ribosomes make proteins, the Golgi apparatus packages and ships molecules, and lysosomes break down waste."),
        q("bio101-2", "BIO 101", "Genetics", 2021, Difficulty.Easy,
            "Which molecule carries the genetic instructions in most living organisms?",
            listOf("DNA", "ATP", "Glucose", "Cellulose"), 0,
            "DNA is like a very long recipe book that tells a living thing how to build and run itself.",
            "DNA stores genetic information as a sequence of four bases: A, T, G and C.",
            "DNA encodes hereditary information in its nucleotide sequence. Genes are transcribed to RNA and translated into proteins.",
            "ATP is an energy carrier, glucose is a sugar fuel, and cellulose is a structural carbohydrate in plant cell walls."),
        q("bio101-3", "BIO 101", "Transport", 2022, Difficulty.Medium,
            "Water moving across a semi-permeable membrane toward the side with the higher solute concentration is called:",
            listOf("Osmosis", "Active transport", "Diffusion of solutes", "Phagocytosis"), 0,
            "Water slips through a special wall toward the side with more stuff dissolved in it, trying to even things out.",
            "That is osmosis: water moves toward the more concentrated solution through a semi-permeable membrane.",
            "Osmosis is net water movement down its water-potential gradient across a selectively permeable membrane, toward higher solute concentration.",
            "Active transport uses energy to move substances against a gradient. 'Diffusion of solutes' describes solute movement, not water. Phagocytosis is a cell engulfing particles."),

        q("eco201-1", "ECO 201", "Demand", 2022, Difficulty.Easy,
            "When the price of a normal good rises and quantity demanded falls, other things equal, this illustrates:",
            listOf("The law of demand", "The law of supply", "Price discrimination", "Perfect competition"), 0,
            "If ice cream gets more expensive, fewer people buy it. Higher price, less buying.",
            "The law of demand says price and quantity demanded move in opposite directions, other things equal.",
            "The downward-sloping demand curve reflects the substitution and income effects: as own price rises, quantity demanded falls, holding other determinants constant.",
            "The law of supply links a higher price to more quantity supplied. Price discrimination is charging different buyers different prices. Perfect competition is a market structure."),
        q("eco201-2", "ECO 201", "Opportunity cost", 2021, Difficulty.Easy,
            "Opportunity cost is best defined as:",
            listOf("The value of the next best alternative given up", "The money price of a good", "The total cost of production", "The cost of borrowing money"), 0,
            "If you spend Saturday at a party, the opportunity cost is the best other thing you could have done, like studying.",
            "Whenever you choose one option, the opportunity cost is the value of the best option you did not choose.",
            "Opportunity cost is the value of the highest-valued forgone alternative. It includes implicit costs and underlies comparative advantage.",
            "The money price and total production cost count only what you pay, not what you give up. The cost of borrowing is the interest rate."),
        q("eco201-3", "ECO 201", "Market structures", 2022, Difficulty.Medium,
            "A market with a single seller and no close substitutes is called a:",
            listOf("Monopoly", "Oligopoly", "Monopolistic competition", "Perfect competition"), 0,
            "If only one shop in town sells something and nothing else works like it, that shop is a monopoly.",
            "A monopoly has one seller, high barriers to entry and no close substitutes.",
            "A monopolist is the sole supplier facing the market demand curve, so it is a price maker and can restrict output to earn supernormal profit.",
            "An oligopoly has a few large sellers, monopolistic competition has many sellers of differentiated products, and perfect competition has many sellers of identical products."),

        // Reasoning practice (labelled a reasoning score, never an IQ score)
        q("apt-num-1", APT_NUM, "Number sequences", null, Difficulty.Easy,
            "What comes next in the sequence 2, 4, 8, 16, ...?",
            listOf("24", "32", "30", "20"), 1,
            "Each number is double the one before it. Double 16 and you get 32.",
            "The rule is 'multiply by 2' each time, so the next term is 16 × 2 = 32.",
            "This is a geometric sequence with common ratio 2, so the next term is 16 × 2 = 32.",
            "24 and 20 add small amounts, and 30 does not follow a constant rule."),
        q("apt-num-2", APT_NUM, "Percentages", null, Difficulty.Easy,
            "What is 15% of 200?",
            listOf("20", "25", "30", "35"), 2,
            "10% of 200 is 20 and 5% is 10. Put them together and 15% is 30.",
            "15/100 × 200 = 30.",
            "A percentage of a quantity is (p/100) × value, so 0.15 × 200 = 30.",
            "20 is 10%, 25 is 12.5% and 35 is 17.5% of 200."),
        q("apt-num-3", APT_NUM, "Ratios", null, Difficulty.Medium,
            "If 3 notebooks cost ₦450, how much do 5 notebooks cost at the same price?",
            listOf("₦650", "₦700", "₦750", "₦800"), 2,
            "First find one notebook: 450 split into 3 is 150. Five notebooks are 5 × 150 = 750.",
            "Unit price = 450 ÷ 3 = ₦150. Five notebooks cost 5 × 150 = ₦750.",
            "Cost is directly proportional to quantity: 450/3 = k = 150 per notebook, so 5k = ₦750.",
            "₦650, ₦700 and ₦800 do not match 5 times the unit price of ₦150."),
    )

    override fun institutions() = institutions
    override fun faculties() = faculties
    override fun departments(facultyId: String) = departments.filter { it.facultyId == facultyId }
    override fun programmes(departmentId: String): List<Programme> =
        departments.filter { it.id == departmentId }.map { Programme("${it.id}-prog", it.id, "${degreeFor(it)} ${it.name}") }
    override fun courses() = courses
    override fun questions(courseCode: String?) =
        if (courseCode == null) questions.filter { it.courseCode != APT_NUM } else questions.filter { it.courseCode == courseCode }
    override fun question(id: String) = questions.firstOrNull { it.id == id }
}
