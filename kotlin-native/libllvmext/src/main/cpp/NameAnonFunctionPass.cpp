#include <llvm/ADT/SmallString.h>
#include <llvm/IR/Module.h>
#include <llvm/Support/MD5.h>
#include <llvm/Pass.h>
#include <llvm/Transforms/Utils/ModuleUtils.h>
#include <NameAnonFunctionPass.h>

using namespace llvm;

// Compute a "unique" hash for the module based on the name of the public
// functions.
class ModuleHasher {
  Module &TheModule;
  std::string TheHash;

public:
  ModuleHasher(Module &M) : TheModule(M) {}

  /// Return the lazily computed hash.
  std::string &get() {
    if (!TheHash.empty())
      // Cache hit :)
      return TheHash;

    MD5 Hasher;
    for (auto &F : TheModule) {
      if (F.isDeclaration() || F.hasLocalLinkage() || !F.hasName())
        continue;
      auto Name = F.getName();
      Hasher.update(Name);
    }
    for (auto &GV : TheModule.globals()) {
      if (GV.isDeclaration() || GV.hasLocalLinkage() || !GV.hasName())
        continue;
      auto Name = GV.getName();
      Hasher.update(Name);
    }

    // Now return the result.
    MD5::MD5Result Hash;
    Hasher.final(Hash);
    SmallString<32> Result;
    MD5::stringifyResult(Hash, Result);
    TheHash = Result.str();
    return TheHash;
  }
};

namespace {
    // Rename all the anon functions in the module
    bool nameUnamedFunctions(Module &M) {
      bool Changed = false;
      ModuleHasher ModuleHash(M);
      int count = 0;
      for (auto &F : M) {
        if (F.hasName())
          continue;
        F.setName(Twine("anon.") + ModuleHash.get() + "." + Twine(count++));
        Changed = true;
      }
      return Changed;
    }

    // Simple pass that provides a name to every anon function.
    class NameAnonFunction : public ModulePass {

    public:
      /// Pass identification, replacement for typeid
      static char ID;

      /// Specify pass name for debug output
    //  StringRef getPassName() const override { return StringRef("Name Anon Functions"); }

      explicit NameAnonFunction() : ModulePass(ID) {}

      bool runOnModule(Module &M) override { return nameUnamedFunctions(M); }
    };

    char NameAnonFunction::ID = 0;
}
//
////INITIALIZE_PASS_BEGIN(NameAnonFunction, "name-anon-functions",
////                      "Provide a name to nameless functions", false, false)
////INITIALIZE_PASS_END(NameAnonFunction, "name-anon-functions",
////                    "Provide a name to nameless functions", false, false)
//

Pass *createNameAnonFunctionPass() { return new NameAnonFunction(); }
