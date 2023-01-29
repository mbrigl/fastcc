// Generated by FastCC v.8.0 - Do not edit this line!

package it.smartio.fastcc.jjtree;

class ASTOptionBinding extends JJTreeNode {

  public ASTOptionBinding(JJTreeParser p, int id) {
    super(p, id);
  }

  private boolean suppressed = false;
  private String  name;

  void initialize(String n, String v) {
    this.name = n;

    // If an option is specific to JJTree it should not be written out
    // to the output file for JavaCC.

    if (JJTreeGlobals.isOptionJJTreeOnly(this.name)) {
      this.suppressed = true;
    }
  }

  @Override
  public String translateImage(Token t) {
    if (this.suppressed) {
      return whiteOut(t);
    } else {
      return t.image;
    }
  }

  @Override
  public final Object jjtAccept(JJTreeParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
// FastCC Checksum=1038F18BB537C064FDDD39FC28EA330F (Do not edit this line!)
// FastCC Options: NODE_FACTORY='', VISITOR_DATA_TYPE='', VISITOR='true', NODE_CLASS='JJTreeNode', NODE_TYPE='ASTOptionBinding', VISITOR_RETURN_TYPE_VOID='false', VISITOR_EXCEPTION='', PARSER_NAME='JJTreeParser', VISITOR_RETURN_TYPE='Object'
