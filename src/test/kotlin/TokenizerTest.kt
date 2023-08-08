package functionPackage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class TokenizerTest {
    @Test
    fun TestCleanFromSpace(){
        assertEquals(listOf("(","12",")"),Tokenizer("  \n \r(  1  \n 2 \n\n)\n\n").tokensStr)
    }
    @Test
    fun TestNumbers(){
        var tokenizer = Tokenizer("0(1.53 - 4)7")
        assertEquals(NumberToken(0.0),tokenizer.GetToken(true))
        tokenizer.Skip()
        assertEquals(NumberToken(1.53),tokenizer.GetToken(true))
        tokenizer.Skip()
        assertEquals(NumberToken(4.0),tokenizer.GetToken(true))
        tokenizer.Skip()
        assertEquals(NumberToken(7.0),tokenizer.GetToken(true))
        assert(tokenizer.isEnd())
    }
    @Test
    fun TestNames(){
        var tokenizer = Tokenizer("x1+y-АБВ_1")
        assertEquals(NameToken("x1"),tokenizer.GetToken(true))
        tokenizer.Skip()
        assertEquals(NameToken("y"),tokenizer.GetToken(true))
        tokenizer.Skip()
        assertEquals(NameToken("АБВ_1"),tokenizer.GetToken(true))
        assert(tokenizer.isEnd())
    }
    @Test
    fun TestKX(){
        var tokenizer = Tokenizer("1x*2+35.2x_3+7z^2")
        assertEquals(NumberToken(1.0),tokenizer.GetToken(true))
        assertEquals(NameToken("*"),tokenizer.GetToken(true))
        assertEquals(NameToken("x"),tokenizer.GetToken(true))
        assertEquals(NameToken("*"),tokenizer.GetToken(true))
        assertEquals(NumberToken(2.0),tokenizer.GetToken(true))
        assertEquals(NameToken("+"),tokenizer.GetToken(true))
        assertEquals(NumberToken(35.2),tokenizer.GetToken(true))
        assertEquals(NameToken("*"),tokenizer.GetToken(true))
        assertEquals(NameToken("x_3"),tokenizer.GetToken(true))
        assertEquals(NameToken("+"),tokenizer.GetToken(true))
        assertEquals(NumberToken(7.0),tokenizer.GetToken(true))
        assertEquals(NameToken("*"),tokenizer.GetToken(true))
        assertEquals(NameToken("z"),tokenizer.GetToken(true))
        assertEquals(NameToken("^"),tokenizer.GetToken(true))
        assertEquals(NumberToken(2.0),tokenizer.GetToken(true))
        assert(tokenizer.isEnd())
    }
    @Test
    fun TestOperators(){
        var tokenizer = Tokenizer("()+-*/^")
        assertEquals(listOf("(",")","+","-","*","/","^"),tokenizer.tokensStr)
        assertEquals(LeftBracket(),tokenizer.GetToken())
    }
    @Test
    fun TestErrors(){
        assertThrows<IncorrectTokenException>{Tokenizer("1.0.2")}
        assertThrows<IncorrectTokenException>{Tokenizer("1.")}
        assertThrows<IncorrectTokenException>{Tokenizer(".x2")}
        assertThrows<IncorrectTokenException>{Tokenizer("x_2.5")}
        assertThrows<IncorrectTokenException>{Tokenizer(".")}
        assertThrows<IncorrectTokenException>{Tokenizer("2_Б")}
        assertThrows<IncorrectTokenException>{Tokenizer("functionPackage.getX.2")}
        assertThrows<IncorrectTokenException>{Tokenizer("2.m")}
        assertThrows<IncorrectTokenException>{Tokenizer("_Б")}
    }
}