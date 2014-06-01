grammar Role;
options {output = AST; }
tokens {
        ROLENAME = 'role';
}

@header {
        package net.driftingsouls.ds2.server.scripting.roles.parser;
        import org.antlr.runtime.Parser;
}

@lexer::header {
        package net.driftingsouls.ds2.server.scripting.roles.parser;
}

@members {
        protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
                throw new MismatchedTokenException(ttype, input);
        }
        
        public void recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow) throws RecognitionException {
                throw e;
        }
}

@rulecatch {
        catch (RecognitionException e) {
                throw e;
        }
}

roleDefinition
        :       ROLENAME IS Identifier (EOL attributeList)? EOF
        ;

attributeList
        :       attribute (EOL attribute)*
        ;

attribute
        :       Identifier IS value
        ;

value
	:	( Number | Text | Location )
	;

Location
	:	Digit+ ':' Digit+ '/' Digit+
	;

Identifier
        :       Letter (Letter | Digit)*
        ;

Number  :       '1'..'9' Digit*
        ;

Numeric :       Digit*
        ;

Text    :       '"' (EscapeChar | ~('"' | '\\'))* '"'
        ;

fragment
EscapeChar
        :       '\\' ('"' | '\\')
        ;

fragment
Letter  :       'a'..'z' | 'A'..'Z'
        ;

fragment
Digit   :       '0'..'9'
        ;

EOL     :       ('\r'? '\n')+
        ;

fragment
SPACE   :       ' ' | '\t'
        ;

IS      :       (':' SPACE*)
        ;