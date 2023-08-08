package functionPackage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
inline fun <reified T:Throwable> myAssertFailsWith(message:String?,block:()->Unit){
    val error = assertFailsWith<T>{block()}
    assertEquals(message,error.message)
}
class ParserTest {
    @Test
    fun testPrepareAll(){
        assertEquals(Tokenizer(
            LeftBracket(),
            RightBracket(),
            binary_operators_map["+"]!!,
            binary_operators_map["-"]!!,
            binary_operators_map["*"]!!,
            binary_operators_map["/"]!!,
            binary_operators_map["^"]!!
        ),Parser.prepareAll("()+-*/^"))
        val tokens = listOf<Token>(
            unary_operators_map["sin"]!!,
            unary_operators_map["cos"]!!,
            unary_operators_map["sec"]!!,
            unary_operators_map["cosec"]!!,
            unary_operators_map["tan"]!!,
            unary_operators_map["cotan"]!!,
            unary_operators_map["asin"]!!,
            unary_operators_map["acos"]!!,
            unary_operators_map["atan"]!!,
            unary_operators_map["exp"]!!,
            unary_operators_map["ln"]!!,
            unary_operators_map["sqrt"]!!
        )
        val resultTokensList = mutableListOf(tokens[0])
        for (token in tokens.subList(1,tokens.size)){
            resultTokensList.add(binary_operators_map["+"]!!)
            resultTokensList.add(token)
        }
        assertEquals(Tokenizer(resultTokensList),Parser.prepareAll(
            "sin+cos+sec+cosec+tan+cotan+asin+acos+atan+exp+ln+sqrt"
        ))
        assertEquals(Tokenizer(
            unary_operators_map["sin"]!!,
            LeftBracket(),
            VT("cos_"),
            binary_operators_map["-"]!!,
            NT(1.0),
            binary_operators_map["*"]!!,
            NT(2.3)
        ),Parser.prepareAll("sin(cos_-1*2.3"))
    }
    @Test
    fun testParseOperandByBracket(){
        assertEquals(
            OT(VT("a"), binary_operators_map["+"]!!,VT("b"),
                OT(VT("c"),
                    OT(VT("e"), binary_operators_map["+"]!!,
                        OT("a")))),
            Parser.parseAllByBrackets("a+b(c(e+(a)))")
        )
        assertEquals(OT(OT(),OT()),Parser.parseAllByBrackets("()()"))
        assertEquals(OT(OT(OT())),Parser.parseAllByBrackets("(())"))
    }
    @Test
    fun testParseOperandByBracketForErrors(){
        assertFailsWith<IncorrectPlacementOfBrackets>{Parser.parseAllByBrackets("(")}
        assertFailsWith<IncorrectPlacementOfBrackets>{Parser.parseAllByBrackets(")")}
        assertFailsWith<IncorrectPlacementOfBrackets>{Parser.parseAllByBrackets("(()")}
        assertFailsWith<IncorrectPlacementOfBrackets>{Parser.parseAllByBrackets("())")}
        assertFailsWith<IncorrectPlacementOfBrackets>{Parser.parseAllByBrackets(")(")}
        assertFailsWith<IncorrectPlacementOfBrackets>{Parser.parseAllByBrackets("((")}
        assertFailsWith<IncorrectPlacementOfBrackets>{Parser.parseAllByBrackets("))")}
        assertFailsWith<IncorrectPlacementOfBrackets>{Parser.parseAllByBrackets("(((a + b ) - d)")}
        assertFailsWith<IncorrectPlacementOfBrackets>{Parser.parseAllByBrackets("((a + b ) - d) )")}
    }
    @Test
    fun testParseOperandByPriority1(){
        //CHECK WITHOUT FIRST MINUS AND BRACKETS
        assertEquals(OT("a",0),Parser.parseAllByPriority(1,"a"))
        assertEquals(OT(
            OT("a",1),
            binary_operators_map["+"]!!,
            OT("b",1)
        ),Parser.parseAllByPriority(1,"a+b"))

        assertEquals(OT(
            VT("a"),
            binary_operators_map["*"]!!,
            VT("b")
        ),Parser.parseAllByPriority(1,"a*b"))

        assertEquals(
            OT(
                OT(
                    VT("a"),
                    binary_operators_map["*"]!!,
                    VT("b"),
                    priority = 1
                ),
                binary_operators_map["-"]!!,
                OT(
                    VT("c"),
                    binary_operators_map["^"]!!,
                    VT("d"),
                    binary_operators_map["/"]!!,
                    VT("q"),
                    priority = 1
                ),
                priority = 0
            ),
            Parser.parseAllByPriority(1,"a*b-c^d/q")
        )

        // CHECK FIRST MINUS
        assertEquals(
            OT(
                OT(-1.0),
                binary_operators_map["*"]!!,
                VT("a"),
                priority = 0
            ),
            Parser.parseAllByPriority(1,"-a")
        )

        assertEquals(
            OT(
                OT(
                    OT(-1.0),
                    binary_operators_map["*"]!!,
                    VT("a"),
                    priority = 1
                ),
                binary_operators_map["+"]!!,
                OT("b",1),
                priority = 0
            ),
            Parser.parseAllByPriority(1,"-a+b")
        )

        // CHECK BRACKETS
        assertEquals(OT("a",0),Parser.parseAllByPriority(1,"((a))"))

        assertEquals(
            OT(
                OT(-1.0),
                binary_operators_map["*"]!!,
                OT(
                    OT("a",1),
                    binary_operators_map["+"]!!,
                    OT("b",1)
                )
            ),
            Parser.parseAllByPriority(1,"-(a+b)")
        )

        assertEquals(
            OT(
                OT(
                    OT("a",1),
                    binary_operators_map["-"]!!,
                    OT("b",1),
                    priority = 1
                ),
                binary_operators_map["-"]!!,
                OT(
                    OT("c",1),
                    binary_operators_map["-"]!!,
                    OT("d",1),
                    priority = 1
                ),
                priority = 0
            ),
            Parser.parseAllByPriority(1,"(((a-b)-(c-d)))")
        )

        //CHECK COMPLEX
        assertEquals(
            OT(
                OT(-1.0),
                binary_operators_map["*"]!!,
                OT(
                    OT(1.0,1),
                    binary_operators_map["+"]!!,
                    OT(
                        OT(
                            OT(-1.0),
                            binary_operators_map["*"]!!,
                            NT(2.0),
                            priority = 1
                        ),
                        binary_operators_map["+"]!!,
                        OT(
                            OT(-1.0),
                            binary_operators_map["*"]!!,
                            OT(3.0),
                            priority = 1
                        ),
                        priority = 1
                    )
                )
            ),
            Parser.parseAllByPriority(1,"-(1+(-2+(-(3))))")
        )
    }
    @Test
    fun testParseOperandByPriority1ForErrors(){

        // CHECK THAT TOKENIZER CAN'T BE EMPTY
        assertFailsWith<EmptyTokenError> {Parser.parseAllByPriority(1,"")}
        assertFailsWith<EmptyTokenError> {Parser.parseAllByPriority(1,"()")  }
        assertFailsWith<EmptyTokenError> {Parser.parseAllByPriority(1,"((()))")  }
        assertFailsWith<EmptyTokenError> {Parser.parseAllByPriority(1,"1+(2+(3+sin()))")  }
        // CHECK THAT EXPRESSION CAN'T START WITH PLUS
        val message1 = OperatorAtStartError("+").message
        myAssertFailsWith<OperatorAtStartError> (message1) { Parser.parseAllByPriority(1, "+") }
        myAssertFailsWith<OperatorAtStartError> (message1) { Parser.parseAllByPriority(1, "+2") }
        myAssertFailsWith<OperatorAtStartError> (message1) { Parser.parseAllByPriority(1, "(2+(3+(+)))") }
        // CHECK THAT THERE IS OPERAND AFTER EACH 1-PRIORITY OPERATOR
        val message2 = MissingOperandAfterOperator("+").message
        val message3 = MissingOperandAfterOperator("-").message
        myAssertFailsWith<MissingOperandAfterOperator> (message3) { Parser.parseAllByPriority(1, "-1-+2") }
        myAssertFailsWith<MissingOperandAfterOperator> (message2) { Parser.parseAllByPriority(1, "-1+2+") }
        myAssertFailsWith<MissingOperandAfterOperator> (message3) { Parser.parseAllByPriority(1, "-+") }
        myAssertFailsWith<MissingOperandAfterOperator> (message2) {
            Parser.parseAllByPriority(
                1,
                "(2+(3+(1+)))"
            )
        }
    }
    @Test
    fun testParseOperandByPriority123(){
        //CHECK Priority 2
        assertEquals(
            OT(
                OT(
                    OT(1.0,2),
                    binary_operators_map["*"]!!,
                    OT("a",2),
                    priority = 1
                ),
                binary_operators_map["+"]!!,
                OT(
                    OT("b",2),
                    binary_operators_map["/"]!!,
                    OT(2.0,2),
                    priority = 1
                )
            ),
            Parser.parseAllByAllPriorities("1*a + b/2")
        )

        //CHECK 1,2,3 PRIORITY CONSISTENCY
        assertEquals(
            OT(
                OT(
                    OT(-1.0,2),
                    binary_operators_map["*"]!!,
                    OT(1.0,2),
                    priority = 1
                ),
                binary_operators_map["+"]!!,
                OT(
                    OT(2.0,2),
                    binary_operators_map["*"]!!,
                    OT(
                        OT(3.0,3),
                        binary_operators_map["^"]!!,
                        OT(4.0,3),
                        priority = 2
                    ),
                    priority = 1
                )
            ),
            Parser.parseAllByAllPriorities("-1+2*3^4")
        )
        //CHECK FOR BRACKETS
        assertEquals(
            OT(
                OT(
                    OT(-1.0,2),
                    binary_operators_map["*"]!!,
                    OT(1.0,2),
                    priority = 3
                ),
                binary_operators_map["^"]!!,
                OT(
                    OT(
                        OT(1.0,1),
                        binary_operators_map["-"]!!,
                        OT(1.0,1),
                        priority = 3
                    ),
                    binary_operators_map["^"]!!,
                    OT(
                        OT(
                            OT(1.0,1),
                            binary_operators_map["-"]!!,
                            OT(1.0,1),
                            priority = 2
                        ),
                        binary_operators_map["/"]!!,
                        OT(1.0,2),
                        priority = 3
                    ),
                    priority = 3
                )
            ),
            Parser.parseAllByAllPriorities("(-1)^(((1-1)^(((1-1)/1))))")
        )
        fun getToken1Pow1(prior:Int):OT{
            return OT(
                OT(1.0,3),
                binary_operators_map["^"]!!,
                OT(1.0,3),
                priority = prior
            )
        }
        val tokenMult = OT(
            getToken1Pow1(2),
            binary_operators_map["*"]!!,
            getToken1Pow1(2),
            priority = 1
        )
        val tokenDiv = OT(
            getToken1Pow1(2),
            binary_operators_map["/"]!!,
            getToken1Pow1(2),
            priority = 1
        )
        //CHECK DEEP BRACKETS
        assertEquals(
            OT(
                OT(
                    tokenMult,
                    binary_operators_map["+"]!!,
                    tokenDiv,
                    priority = 3
                ),
                binary_operators_map["^"]!!,
                OT(
                    tokenMult,
                    binary_operators_map["-"]!!,
                    tokenDiv,
                    priority = 3
                )
            ),
            Parser.parseAllByAllPriorities("(((1^1)*(1^1))+((1^1)/(1^1)))^(((1^1)*(1^1))-((1^1)/(1^1)))")
        )
    }
    @Test
    fun testParseAllByAllPrioritiesForErrors(){
        //CHECK FOR START ERROR
        myAssertFailsWith<OperatorAtStartError> (
            OperatorAtStartError("*").message
        ) { Parser.parseAllByAllPriorities("-1+(*2)") }
        myAssertFailsWith<OperatorAtStartError> (
            OperatorAtStartError("/").message
        ) { Parser.parseAllByAllPriorities("(((/2)))") }
        myAssertFailsWith<OperatorAtStartError> (
            OperatorAtStartError("^").message
        ) { Parser.parseAllByAllPriorities("(((^)))") }

        //CHECK FOR MISSED OPERAND
        myAssertFailsWith<MissingOperandAfterOperator> (
            MissingOperandAfterOperator("*").message
        ) { Parser.parseAllByAllPriorities("(1+2+(3*))") }
        myAssertFailsWith<MissingOperandAfterOperator> (
            MissingOperandAfterOperator("/").message
        ) { Parser.parseAllByAllPriorities("1//2") }
        myAssertFailsWith<MissingOperandAfterOperator> (
            MissingOperandAfterOperator("^").message
        ) { Parser.parseAllByAllPriorities(("((1^-2))")) }
    }
    @Test
    fun testCleanFromDivAndMinus(){
        assertEquals(
            OT(
                OT(
                    OT(1.0,2),
                    binary_operators_map["*"]!!,
                    OT(
                        OT(1.0,3),
                        binary_operators_map["^"]!!,
                        OT(-1.0,3),
                        priority = 2
                    ),
                    priority = 1
                ),
                binary_operators_map["+"]!!,
                OT(
                    OT(1.0,2),
                    binary_operators_map["*"]!!,
                    OT(-1.0,2),
                    priority = 1
                )
            ),
            Parser.cleanAll("1/1-1")
        )
    }
    private fun test_f(s:String){
        assertEquals(Parser.parseAll(s,false).asStr(),s)
    }
    private fun test_f(s:String, expected:String){
        println(Parser.parseAll(s,false))
        assertEquals(Parser.parseAll(s,false).asStr(),expected)
    }
    private fun test_t(s:String){
        assertEquals(Parser.parseAll(s,true).asStr(),s)
    }
    private fun test_t(s:String, expected:String){
        assertEquals(Parser.parseAll(s,true).asStr(),expected)
    }
    @Test
    fun testParseForBinaryMode(){
        fun test(s:String){
            test_f(s)
        }
        fun test(s:String,expected:String){
            test_f(s,expected)
        }
        //TEST PRIMITIVE
        test("a")
        test("1")
        test("1.2")
        test("(a)","a")

        //TEST ONLY BINARY OPERATORS
        test("a+b","+(a,b)")
        test("a*b","*(a,b)")
        test("a^b","^(a,b)")
        test("a-b","+(a,*(b,-1))")
        test("a/b","*(a,^(b,-1))")

        //TEST ONLY UNARY OPERATORS
        test("sin(a)")
        test("cos(a)")
        test("tan(a)")
        test("cotan(a)")
        test("sec(a)")
        test("cosec(a)")
        test("asin(a)")
        test("acos(a)")
        test("atan(a)")
        test("exp(a)")
        test("ln(a)")
        test("sqrt(a)")

        //TEST ASSOCIATIVITY
        test("a+b+c","+(+(a,b),c)")
        test("a*b*c","*(*(a,b),c)")
        test("a^b^c","^(a,^(b,c))")

        //TEST COMPOSITION
        test("sin(cos(a))")
        test("sin(1+2+3x)","sin(+(3,*(3,x)))")

        //COMPLEX TEST
        test(
            "sin(3x)+(ln(x-1)+tan(x))/(x^3+exp(x+3))^(1/4)",
            "+(sin(*(3,x)),*(+(ln(+(x,-1)),tan(x)),^(^(+(^(x,3),exp(+(x,3))),0.25),-1)))"
        )
        test(
            "((a+b)^(c*d))+(e+f)+(((g^h)^i)*(j*k)*(l+m))",
            "+(+(^(+(a,b),*(c,d)),+(e,f)),*(*(^(^(g,h),i),*(j,k)),+(l,m)))"
        )
    }
    @Test
    fun testParseOperandForPolyMode(){
        fun test(s:String){
            test_t(s)
        }
        fun test(s:String,expected:String){
            test_t(s,expected)
        }
        //TEST PRIMITIVE
        test("a")
        test("1")
        test("1.2")
        test("(a)","a")

        //TEST ONLY BINARY OPERATORS
        test("a+b","+(a,b)")
        test("a*b","*(a,b)")
        test("a^b","^(a,b)")
        test("a-b","+(a,*(b,-1))")
        test("a/b","*(a,^(b,-1))")

        //TEST ONLY UNARY OPERATORS
        test("sin(a)")
        test("cos(a)")
        test("tan(a)")
        test("cotan(a)")
        test("sec(a)")
        test("cosec(a)")
        test("asin(a)")
        test("acos(a)")
        test("atan(a)")
        test("exp(a)")
        test("ln(a)")
        test("sqrt(a)")

//        //TEST ASSOCIATIVITY
//        test("a+b+c","+(a,b,c)")
//        test("a*b*c","*(a,b,c)")
//        test("a^b^c","^(a,^(b,c))")
//
//        //TEST COMPOSITION
//        test("sin(cos(a))")
//        test("sin(1+2+3x)","sin(+(1,2,*(3,x)))")
//
//        //COMPLEX TEST
//        test(
//            "sin(3x)+(ln(x-1)+tan(x))/(x^3+exp(x+3))^(1/4)",
//            "+(sin(*(3,x)),*(+(ln(+(x,-1)),tan(x)),^(^(+(^(x,3),exp(+(x,3))),0.25),-1)))"
//        )
//        //TEST FOR ANY ORDER OF OPERATORS
//        test(
//            "((a+b)^(c*d))+(e+f)+(((g^h)^i)*(j*k)*(l+m))",
//            "+(^(+(a,b),*(c,d)),+(e,f),*(^(^(g,h),i),*(j,k),+(l,m)))"
//        )
    }

    @Test
    fun testParseOperandForBinaryModeForErrors(){
        myAssertFailsWith<UnaryOperatorIsNotOperand>(
            UnaryOperatorIsNotOperand("sin").message
        ) { Parser.parseAll("sin", false) }
        myAssertFailsWith<UnaryOperatorIsNotOperand>(
            UnaryOperatorIsNotOperand("sin").message
        ) { Parser.parseAll("(sin)", false) }
        myAssertFailsWith<UnaryOperatorIsNotOperand>(
            UnaryOperatorIsNotOperand("cos").message
        ) { Parser.parseAll("sin(cos)", false) }
        myAssertFailsWith<OnlyUnaryOperatorAppliesToOneArgument>(
            OnlyUnaryOperatorAppliesToOneArgument(NT(1.0)).message
        ) { Parser.parseAll("1(sin)", false) }
        myAssertFailsWith<OnlyUnaryOperatorAppliesToOneArgument>(
            OnlyUnaryOperatorAppliesToOneArgument(VT("x")).message
        ) { Parser.parseAll("x(sin)", false) }
        myAssertFailsWith<OnlyUnaryOperatorAppliesToOneArgument>(
            OnlyUnaryOperatorAppliesToOneArgument(OT("x")).message
        ) { Parser.parseAll("(x)(sin)", false) }
        myAssertFailsWith<OnlyUnaryOperatorAppliesToOneArgument>(
            OnlyUnaryOperatorAppliesToOneArgument(OT("x")).message
        ) { Parser.parseAll("(x)1", false) }
        myAssertFailsWith<SecondTokenShouldBeOperator>(
            SecondTokenShouldBeOperator(OT("y")).message
        ) { Parser.parseAll("x(y)z", false) }
        myAssertFailsWith<SecondTokenShouldBeOperator>(
            SecondTokenShouldBeOperator(OT("y")).message
        ) { Parser.parseAll("sin(y)z", false) }
        myAssertFailsWith<UnaryOperatorIsNotOperand>(
            UnaryOperatorIsNotOperand("sin").message
        ) { Parser.parseAll("x(sin)z", false) }
        myAssertFailsWith<SecondTokenShouldBeOperator>(
            SecondTokenShouldBeOperator(OT("y")).message
        ) { Parser.parseAll("(x)(y)z", false) }
        myAssertFailsWith<SecondTokenShouldBeOperator>(
            SecondTokenShouldBeOperator(VT("y")).message
        ) { Parser.parseAll("(x)y(z)", false) }
    }
    @Test
    fun testParseOperandForPolyModeForErrors(){
        myAssertFailsWith<UnaryOperatorIsNotOperand>(
            UnaryOperatorIsNotOperand("sin").message
        ) { Parser.parseAll("sin", true) }
        myAssertFailsWith<UnaryOperatorIsNotOperand>(
            UnaryOperatorIsNotOperand("sin").message
        ) { Parser.parseAll("(sin)", true) }
        myAssertFailsWith<UnaryOperatorIsNotOperand>(
            UnaryOperatorIsNotOperand("cos").message
        ) { Parser.parseAll("sin(cos)", true) }
        myAssertFailsWith<OnlyUnaryOperatorAppliesToOneArgument>(
            OnlyUnaryOperatorAppliesToOneArgument(NT(1.0)).message
        ) { Parser.parseAll("1(sin)", true) }
        myAssertFailsWith<OnlyUnaryOperatorAppliesToOneArgument>(
            OnlyUnaryOperatorAppliesToOneArgument(VT("x")).message
        ) { Parser.parseAll("x(sin)", true) }
        myAssertFailsWith<OnlyUnaryOperatorAppliesToOneArgument>(
            OnlyUnaryOperatorAppliesToOneArgument(OT("x")).message
        ) { Parser.parseAll("(x)(sin)", true) }
        myAssertFailsWith<OnlyUnaryOperatorAppliesToOneArgument>(
            OnlyUnaryOperatorAppliesToOneArgument(OT("x")).message
        ) { Parser.parseAll("(x)1", true) }
        myAssertFailsWith<SecondTokenShouldBeOperator>(
            SecondTokenShouldBeOperator(OT("y")).message
        ) { Parser.parseAll("x(y)z", true) }
        myAssertFailsWith<SecondTokenShouldBeOperator>(
            SecondTokenShouldBeOperator(OT("y")).message
        ) { Parser.parseAll("sin(y)z", true) }
        myAssertFailsWith<UnaryOperatorIsNotOperand>(
            UnaryOperatorIsNotOperand("sin").message
        ) { Parser.parseAll("x(sin)z", true) }
        myAssertFailsWith<SecondTokenShouldBeOperator>(
            SecondTokenShouldBeOperator(OT("y")).message
        ) { Parser.parseAll("(x)(y)z", true) }
        myAssertFailsWith<SecondTokenShouldBeOperator>(
            SecondTokenShouldBeOperator(VT("y")).message
        ) { Parser.parseAll("(x)y(z)", true) }
    }

}
